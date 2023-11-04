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

  Process("php" :: "-r" :: codeStr :: Nil).!(ProcessLogger(report.info(_)))
  '{}
}

inline def php[A](
  inline a: A
): String = ${ phpImpl('a) }

def phpImpl[A](
  e: Expr[A]
)(
  using q: Quotes
): Expr[String] = Expr {

  import quotes.reflect.*

  val code: String = render(translate(e.asTerm))

  s"""<?php
     |$code
     |""".stripMargin
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

    case Assign(lhs, rhs)           => E.Assign(translate(lhs), translate(rhs))
    case Literal(UnitConstant())    => E.Blank
    case Literal(StringConstant(v)) => E.StringLiteral(v)
    case Literal(IntConstant(v))    => E.IntLiteral(v)
    case ValDef(name, _, Some(v))   => E.Assign(E.Ident(name), translate(v))
    case DefDef(name, List(TermParamClause(List(ValDef(argName, _, None)))), _, Some(body)) =>
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

      val finalStat: E => E = {
        case e: E.Echo => e
        case e         => E.Return(e)
      }

      E.FunctionDef(
        name,
        globals.toList,
        argName,
        translate(body) match {
          case E.Block(stats) =>
            if (stats.nonEmpty) {
              E.Block(stats.init.appended(finalStat(stats.last)))
            } else
              E.Block(stats)
          case other => E.Block(finalStat(other) :: Nil)
        },
      )
    case Apply(f, List(arg)) => E.Apply(translate(f), translate(arg))
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

  case Echo(
    arg: E
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
    argName: String,
    body: E,
  )

  case Apply(
    f: E,
    arg: E,
  )

  def concat(
    right: E
  ): E = BinOp(this, ".", right)

}

def render(
  e: E
): String =
  e match {
    case E.Blank                => ""
    case E.Return(e)            => s"return ${render(e)}"
    case E.IntLiteral(value)    => value.toString
    case E.StringLiteral(value) => '"' + value + '"'
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
    case E.Ident(name)            => s"$$$name"
    case E.Echo(arg)              => "echo " + render(arg)
    case E.BinOp(left, op, right) => s"${render(left)} $op ${render(right)}"
    case E.FunctionDef(name, globals, argName, body) =>
      val globalsString =
        if (globals.isEmpty)
          ""
        else
          globals.map("$" + _).mkString("global ", ", ", ";\n")

      val bodyString = globalsString + render(body)

      s"""|function $name($$$argName) {
          |${bodyString.indentTrim(2)}
          |}""".stripMargin
    case E.Apply(f, arg) => s"${render(f).stripPrefix("$")}(${render(arg)})"
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
