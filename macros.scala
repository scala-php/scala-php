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
    s.isOwnedWithin(Symbol.spliceOwner)
  }

  // returns true if `s` has an owner within `scope`
  def isOwnedWithin(
    scope: q.reflect.Symbol
  ): Boolean = !allOwners(s).contains(scope)

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

  def function(
    name: Option[String],
    args: List[String],
    body: Tree,
    scope: Symbol,
  ) = {
    object variableReferences extends TreeAccumulator[Set[Ident]] {
      override def foldTree(
        x: Set[Ident],
        tree: Tree,
      )(
        owner: Symbol
      ): Set[Ident] =
        tree match {
          case ident: Ident
              if (tree.symbol.isOwnedWithin(scope))
                && tree.symbol.isValDef =>
            x + ident
          case other => foldOverTree(x, other)(owner)
        }
    }

    val globals =
      variableReferences
        .foldOverTree(Set.empty, body)(body.symbol)
        .map(_.name) - "println" - "_root_"

    E.FunctionDef(
      name,
      args,
      globals.toList,
      translate(body).ensureBlock.returned,
    )
  }

  e match {
    case Inlined(_, _, e) => translate(e)
    case b @ Block(
          List(d @ DefDef(name, List(TermParamClause(args)), _, Some(body))),
          Closure(Ident(n), _),
        ) if d.symbol.isAnonymousFunction && n == name =>
      function(
        // anon functions can't have names
        name = None,
        args = args.map(_.name),
        body = body,
        d.symbol,
      )
    case DefDef(name, List(TermParamClause(args)), _, Some(body)) =>
      function(Some(name), args.map(_.name), body, e.symbol)
    // when something like a ValDef is used as the only expression in a block
    case Block(List(stat), Literal(UnitConstant())) => E.Unit(translate(stat))
    case Block(stats, expr)                         => E.Block(stats.appended(expr).map(translate))
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
        E.BinOp(
          translate(e1),
          op,
          translate(e2),
        )
      else
        report.errorAndAbort("couldn't concat values of type " + e1.tpe)

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

    case Typed(e, _) => translate(e)
    case Assign(lhs, rhs) =>
      E.Unit(
        E.Assign(translate(lhs), translate(rhs))
      )
    case Literal(UnitConstant())     => E.Unit(E.Blank)
    case Literal(StringConstant(v))  => E.StringLiteral(v)
    case Literal(IntConstant(v))     => E.IntLiteral(v)
    case Literal(BooleanConstant(v)) => E.BooleanLiteral(v)
    case ValDef(name, _, Some(v))    => E.Assign(E.VariableIdent(name), translate(v))
    case If(cond, thenp, elsep) =>
      E.If(
        cond = translate(cond),
        thenp = translate(thenp),
        elsep = translate(elsep),
      )
    case Select(Ident("StdIn"), "readLine") => E.Builtin("readline")
    case Select(a, "apply")                 => translate(a)
    case Apply(Ident("println"), Nil)       => E.Echo(E.StringLiteral("\\n"))
    case Apply(Ident("println"), List(msg)) =>
      E.Echo(
        translate(msg)
          .unblockOr(
            report.errorAndAbort(
              "Block parameters aren't supported: " + msg.structure,
              msg.pos,
            )
          )
          .concat(E.StringLiteral("\\n"))
      )

    case Apply(f, args) =>
      E.Apply(
        translate(f),
        args.map(arg =>
          translate(arg).unblockOr(
            report.errorAndAbort(
              "Block parameters aren't supported: " + arg.structure,
              arg.pos,
            )
          )
        ),
      )
    case other => report.errorAndAbort(s"Unsupported code (${other.show}): " + other.structure)
  }
}

extension (
  using q: Quotes
)(
  tree: q.reflect.Tree
) {

  def structure = {
    import q.reflect.*
    tree.show(
      using Printer.TreeStructure
    )
  }

}

enum E {

  case Builtin(
    name: String
  )

  case Unit(
    e: E
  )

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
    name: Option[String],
    argNames: List[String],
    useRefs: List[String],
    body: E,
  )

  case Globals(
    names: List[String]
  )

  case Apply(
    f: E,
    args: List[E],
  )

  def concat(
    right: E
  ): E = BinOp(this, ".", right)

  def ensureBlock: E.Block =
    this match {
      case b: Block => b
      case _        => Block(this :: Nil)
    }

  def unblockOr(
    fallback: => E
  ): E =
    this match {
      case Block(List(stat)) => stat
      case Block(_)          => fallback
      case other             => other
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

  def prefixAsBlock(
    another: E
  ): Block =
    this match {
      case Block(stats) => Block(another :: stats)
      case other        => Block(another :: other :: Nil)
    }

}

def escape(
  name: String
) = name.replace("$", "DOLLAR")

given ToExpr[E] with {

  def apply(
    x: E
  )(
    using Quotes
  ): Expr[E] =
    x match {
      case E.Unit(e)               => '{ E.Unit(${ apply(e) }) }
      case E.Blank                 => '{ E.Blank }
      case E.Builtin(name)         => '{ E.Builtin(${ Expr(name) }) }
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
      case E.Globals(names)      => '{ E.Globals(${ Expr.ofList(names.map(Expr(_))) }) }
      case E.FunctionDef(name, argNames, useRefs, body) =>
        '{
          E.FunctionDef(
            ${ Expr(name) },
            ${ Expr.ofList(argNames.map(Expr(_))) },
            ${ Expr.ofList(useRefs.map(Expr(_))) },
            ${ apply(body) },
          )
        }
      case E.Apply(f, args) => '{ E.Apply(${ apply(f) }, ${ Expr.ofList(args.map(apply)) }) }
    }

}

private def renderStat(
  e: E
): String =
  e match {
    case _: E.FunctionDef | E.Blank | _: E.If | _: E.Block => render(e)
    case _                                                 => render(e) + ";"
  }

def renderPublic(
  e: E
): String = renderStdlib + render(e, topLevel = true)

private val T_PAAMAYIM_NEKUDOTAYIM = "::"

private def renderStdlib =
  s"""|//
      |// scala.php stdlib start
      |//
      |class scala_Unit implements Stringable{
      |  public function __toString() {
      |    return "()";
      |  }
      |
      |  public static function consume() {
      |    return self${T_PAAMAYIM_NEKUDOTAYIM}getInstance();
      |  }
      |
      |  private static $$instance = null;
      |  public static function getInstance() {
      |    if (self${T_PAAMAYIM_NEKUDOTAYIM}$$instance === null) {
      |      self${T_PAAMAYIM_NEKUDOTAYIM}$$instance = new self();
      |    }
      |    return self${T_PAAMAYIM_NEKUDOTAYIM}$$instance;
      |  }
      |}
      |
      |//
      |// scala.php stdlib end
      |//
      |
      |""".stripMargin

private def render(
  e: E,
  topLevel: Boolean = false,
): String =
  e match {
    case E.Blank                 => ""
    case E.Unit(expr)            => s"scala_Unit${T_PAAMAYIM_NEKUDOTAYIM}consume(${render(expr)})"
    case E.Builtin(name)         => name
    case E.Return(e)             => s"return ${render(e)}"
    case E.IntLiteral(value)     => value.toString
    case E.StringLiteral(value)  => '"' + value + '"'
    case E.BooleanLiteral(value) => value.toString()
    case E.Block(stats) =>
      val bodyRendered = stats
        .map(renderStat)
        .mkString("\n")

      if (topLevel)
        bodyRendered
      else
        s"""|{
            |${bodyRendered.indentTrim(2)}
            |}""".stripMargin
    case E.Assign(lhs, rhs)       => render(lhs) + " = " + render(rhs)
    case E.VariableIdent(name)    => s"$$${escape(name)}"
    case E.Ident(name)            => name
    case E.Echo(arg)              => "echo " + render(arg)
    case E.BinOp(left, op, right) => s"${render(left)} $op ${render(right)}"
    case E.Globals(names) =>
      if (names.isEmpty)
        ""
      else
        "global " + names.map("$" + _).mkString(", ")

    case E.FunctionDef(name, argNames, useRefs, body) =>
      val paramString = argNames.map(E.VariableIdent(_)).map(render(_)).mkString(", ")

      val nameText = name.getOrElse("")

      val useText =
        if (name.nonEmpty || useRefs.isEmpty)
          ""
        else
          " use (" + useRefs.map("&$" + _).mkString(", ") + ")"

      val bodyNode =
        if (name.isEmpty || useRefs.isEmpty)
          body
        else
          body.prefixAsBlock(E.Globals(useRefs.toList))

      val bodyString = render(bodyNode)

      s"""|function $nameText(${paramString})$useText $bodyString""".stripMargin
    case E.Apply(f, args) => s"${render(f)}(${args.map(render(_)).mkString(", ")})"
    case E.If(cond, thenp, elsep) =>
      s"if (${render(cond)}) ${renderStat(thenp)} else ${renderStat(elsep)}"
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
      e.asTerm.structure,
    e.asTerm.pos,
  )

  e
}
