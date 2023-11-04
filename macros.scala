import java.nio.file.Files
import java.nio.file.Paths
import scala.quoted.ToExpr
import scala.quoted._

inline def runPHP(
  inline code: String
): Unit = ${ runPHPImpl('code) }

def runPHPImpl(
  code: Expr[String]
)(
  using q: Quotes
): Expr[Unit] = {
  import quotes.reflect.*

  val codeStr = code.valueOrAbort

  import sys.process.*
  // Files.writeString(Paths.get("demo.php"), codeStr)

  // report.info(Process("php" :: "demo.php" :: Nil).!!, code.asTerm.pos)
  '{}
}

inline def php[A](
  inline a: A
): E = ${ phpImpl('a) }

def phpImpl[A](
  e: Expr[A]
)(
  using q: Quotes
): Expr[E] = Expr {

  import quotes.reflect.*

  translate(e.asTerm)
}

def allOwners(
  using q: Quotes
)(
  s: q.reflect.Symbol
): List[q.reflect.Symbol] = {
  import quotes.reflect.*
  if (s.maybeOwner == Symbol.noSymbol)
    Nil
  else
    s.owner :: allOwners(s.owner)
}

extension (
  using q: Quotes
)(
  s: q.reflect.Symbol
) {

  def definedOutsideMacroCall: Boolean = {
    import q.reflect.*
    !allOwners(s).contains(Symbol.spliceOwner)
  }

}

def translate(
  using q: Quotes
)(
  e: q.reflect.Tree
): E = {
  import quotes.reflect.*

  object StringContextApply {
    def unapply(
      t: Tree
    ): Option[
      (
        List[Term],
        List[Term],
      )
    ] =
      t match {
        case Apply(
              Select(
                Apply(
                  Select(Select(Select(Ident("_root_"), "scala"), "StringContext"), "apply"),
                  List(Typed(Repeated(constantParts, _), _)),
                ),
                "s",
              ),
              List(Typed(Repeated(args, _), _)),
            ) =>
          Some((constantParts, args))
        case _ => None
      }
  }

  e match {
    case Inlined(_, _, e)   => translate(e)
    case Block(stats, expr) => E.Block(stats.appended(expr).map(translate))
    case Ident(s) =>
      if (e.symbol.definedOutsideMacroCall)
        report.errorAndAbort("Cannot refer to symbols defined outside the macro call", e.pos)

      if (e.symbol.isValDef)
        E.VariableIdent(s)
      else
        E.Ident(s)

    case Apply(Select(e1, op @ ("+" | "-" | "*" | "/")), List(e2)) =>
      if (e1.tpe <:< TypeRepr.of[String])
        translate(e1).concat(translate(e2))
      else if (e1.tpe <:< TypeRepr.of[Int])
        E.BinOp(translate(e1), op, translate(e2))
      else
        report.errorAndAbort("couldn't concat values of type " + e1.tpe)

    case Apply(Ident("println"), Nil)       => E.Echo(E.StringLiteral("\\n"))
    case Apply(Ident("println"), List(msg)) => E.Echo(translate(msg).concat(E.StringLiteral("\\n")))
    case StringContextApply(constantParts, args) =>
      val parts = constantParts
        .map { case Literal(StringConstant(s)) => s }
        .map(StringContext.processEscapes(_))
        .map(E.StringLiteral(_))

      val pieces = parts.head :: args.map(translate(_)).zip(parts.tail).flatMap(_.toList)

      pieces
        .filterNot {
          _ == E.StringLiteral("")
        }
        .reduceLeft(_ concat _)

    case Typed(e, _)                 => translate(e)
    case Assign(lhs, rhs)            => E.Assign(translate(lhs), translate(rhs))
    case Literal(UnitConstant())     => E.Blank
    case Literal(StringConstant(v))  => E.StringLiteral(v)
    case Literal(IntConstant(v))     => E.IntLiteral(v)
    case Literal(BooleanConstant(v)) => E.BooleanLiteral(v)
    case ValDef(name, _, Some(v))    => E.Assign(E.VariableIdent(name), translate(v))
    case If(cond, thenp, elsep) =>
      E.If(
        cond = translate(cond),
        thenp = translate(thenp).ensureBlock,
        elsep = translate(elsep).ensureBlock,
      )
    case DefDef(name, List(TermParamClause(args)), _, Some(body)) =>
      object variableReferences extends TreeAccumulator[Set[String]] {
        override def foldTree(
          x: Set[String],
          tree: Tree,
        )(
          owner: Symbol
        ): Set[String] =
          tree match {
            case Ident(name)
                // only symbols defined outside the current function
                if tree.symbol.owner != e.symbol
                  && tree.symbol.isValDef =>
              x + name
            case other => foldOverTree(x, other)(owner)
          }
      }

      val referencedVariables =
        variableReferences.foldOverTree(Set.empty, body)(body.symbol) - "println" - "_root_"

      val globals = referencedVariables

      E.FunctionDef(
        name,
        globals.toList,
        args.map(_.name),
        translate(body).ensureBlock.returned,
      )
    case Apply(f, args) => E.Apply(translate(f), args.map(translate))
    case other =>
      report.errorAndAbort(
        "Unsupported code: " +
          other
            .show(
              using Printer.TreeStructure
            )
      )
  }
}

enum E {

  case Blank

  case BooleanLiteral(
    value: Boolean
  )

  case StringLiteral(
    value: String
  )

  case IntLiteral(
    value: Int
  )

  case Return(
    e: E
  )

  case Block(
    stats: List[E]
  )

  case BinOp(
    left: E,
    op: String,
    right: E,
  )

  case If(
    cond: E,
    thenp: E,
    elsep: E,
  )

  case Echo(
    arg: E
  )

  case VariableIdent(
    name: String
  )

  case Ident(
    name: String
  )

  case Assign(
    lhs: E,
    rhs: E,
  )

  case FunctionDef(
    name: String,
    globals: List[String],
    argNames: List[String],
    body: E,
  )

  case Apply(
    f: E,
    args: List[E],
  )

  def concat(
    right: E
  ): E = BinOp(this, ".", right)

  def ensureBlock: E =
    this match {
      case b: Block => b
      case _        => Block(this :: Nil)
    }

  def returned: E =
    this match {
      case If(cond, thenp, elsep) => If(cond, thenp.returned, elsep.returned)
      case Block(stats) =>
        stats.lastOption match {
          case Some(last) => Block(stats.init.appended(last.returned))
          case None       => Block(stats)
        }
      case _: Echo => this
      case other   => Return(other)
    }

}

given ToExpr[E] with {

  def apply(
    x: E
  )(
    using Quotes
  ): Expr[E] =
    x match {
      case E.Blank                 => '{ E.Blank }
      case E.BooleanLiteral(value) => '{ E.BooleanLiteral(${ Expr(value) }) }
      case E.StringLiteral(value)  => '{ E.StringLiteral(${ Expr(value) }) }
      case E.IntLiteral(value)     => '{ E.IntLiteral(${ Expr(value) }) }
      case E.Return(e)             => '{ E.Return(${ apply(e) }) }
      case E.Block(stats)          => '{ E.Block(${ Expr.ofList(stats.map(apply)) }) }
      case E.BinOp(left, op, right) =>
        '{ E.BinOp(${ apply(left) }, ${ Expr(op) }, ${ apply(right) }) }
      case E.If(cond, thenp, elsep) =>
        '{ E.If(${ apply(cond) }, ${ apply(thenp) }, ${ apply(elsep) }) }
      case E.Echo(arg)           => '{ E.Echo(${ apply(arg) }) }
      case E.VariableIdent(name) => '{ E.VariableIdent(${ Expr(name) }) }
      case E.Ident(name)         => '{ E.Ident(${ Expr(name) }) }
      case E.Assign(lhs, rhs)    => '{ E.Assign(${ apply(lhs) }, ${ apply(rhs) }) }
      case E.FunctionDef(name, globals, argNames, body) =>
        '{
          E.FunctionDef(
            ${ Expr(name) },
            ${ Expr.ofList(globals.map(Expr(_))) },
            ${ Expr.ofList(argNames.map(Expr(_))) },
            ${ apply(body) },
          )
        }
      case E.Apply(f, args) => '{ E.Apply(${ apply(f) }, ${ Expr.ofList(args.map(apply)) }) }
    }

}

def render(
  e: E
): String =
  e match {
    case E.Blank                 => ""
    case E.Return(e)             => s"return ${render(e)}"
    case E.IntLiteral(value)     => value.toString
    case E.StringLiteral(value)  => '"' + value + '"'
    case E.BooleanLiteral(value) => value.toString()
    case E.Block(stats) =>
      stats
        .map { e =>
          e match {
            case _: E.FunctionDef | E.Blank => render(e)
            case _                          => render(e) + ";"
          }
        }
        .mkString("\n")
    case E.Assign(lhs, rhs)       => render(lhs) + " = " + render(rhs)
    case E.VariableIdent(name)    => s"$$$name"
    case E.Ident(name)            => name
    case E.Echo(arg)              => "echo " + render(arg)
    case E.BinOp(left, op, right) => s"${render(left)} $op ${render(right)}"
    case E.FunctionDef(name, globals, argNames, body) =>
      val globalsString =
        if (globals.isEmpty)
          ""
        else
          globals.map("$" + _).mkString("global ", ", ", ";\n")

      val bodyString = globalsString + render(body)

      s"""|function $name(${argNames.map(E.VariableIdent(_)).map(render).mkString(", ")}) {
          |${bodyString.indentTrim(2)}
          |}""".stripMargin
    case E.Apply(f, args) => s"${render(f)}(${args.map(render).mkString(", ")})"
    case E.If(cond, thenp, elsep) => s"""|if (${render(cond)}) {
                                         |${render(thenp).indentTrim(2)}
                                         |} else {
                                         |${render(elsep).indentTrim(2)}
                                         |}""".stripMargin
  }

extension (
  s: String
) {

  def indentTrim(
    chars: Int
  ): String = s
    .linesIterator
    .map { l =>
      if (l.isBlank)
        l
      else
        " " * chars + l
    }
    .mkString("\n")

}

inline def log[A](
  inline a: A
): A = ${ logImpl('a) }

def logImpl[A](
  e: Expr[A]
)(
  using q: Quotes
): Expr[A] = {
  import quotes.reflect.*

  report.info(
    e.show + ": " +
      e.asTerm
        .show(
          using Printer.TreeStructure
        ),
    e.asTerm.pos,
  )

  e
}
