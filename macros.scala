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

  val code: String = render(transpile(e.asTerm))

  s"""<?php
     |$code
     |""".stripMargin
}

def transpile(
  using q: Quotes
)(
  e: q.reflect.Tree
): E = {
  import quotes.reflect.*

  e match {
    case Inlined(_, _, e)   => transpile(e)
    case Block(stats, expr) => E.Block(stats.map(transpile), transpile(expr))
    case Ident(s)           => E.Ident(s)
    case Apply(Select(e1, "+"), List(e2)) =>
      if (e1.tpe <:< TypeRepr.of[String])
        E.StringConcat(transpile(e1), transpile(e2))
      else if (e1.tpe <:< TypeRepr.of[Int])
        E.Addition(transpile(e1), transpile(e2))
      else
        report.errorAndAbort("couldn't concat values of type " + e1.tpe)

    case Apply(Ident("println"), Nil) => E.Echo(E.StringLiteral("\\n"))
    case Apply(Ident("println"), List(msg)) =>
      E.Echo(E.StringConcat(transpile(msg), E.StringLiteral("\\n")))
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
      val parts = constantParts
        .map { case Literal(StringConstant(s)) => s }
        .map(StringContext.processEscapes(_))
        .map(E.StringLiteral(_))
      val argz = args.map(transpile(_))

      val pieces = parts.head :: argz.zip(parts.tail).flatMap(_.toList)

      pieces
        .filterNot {
          _ == E.StringLiteral("")
        }
        .reduceLeft(E.StringConcat(_, _))

    case Assign(lhs, rhs)           => E.Assign(transpile(lhs), transpile(rhs))
    case Literal(UnitConstant())    => E.Blank
    case Literal(StringConstant(v)) => E.StringLiteral(v)
    case Literal(IntConstant(v))    => E.IntLiteral(v)
    case ValDef(name, _, Some(v))   => E.Assign(E.Ident(name), transpile(v))
    case DefDef(name, List(TermParamClause(List(ValDef(argName, _, None)))), _, Some(body)) =>
      E.FunctionDef(name, argName, transpile(body))
    case Apply(f, List(arg)) => E.Apply(transpile(f), transpile(arg))
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

  case Block(
    stats: List[E],
    expr: E,
  )

  case StringConcat(
    left: E,
    right: E,
  )

  case Addition(
    left: E,
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
    argName: String,
    body: E,
  )

  case Apply(
    f: E,
    arg: E,
  )

}

def render(
  e: E,
  returnLast: Boolean = false,
): String =
  e match {
    case E.Blank                => ""
    case E.IntLiteral(value)    => value.toString
    case E.StringLiteral(value) => '"' + value + '"'
    case E.Block(stats, e) =>
      stats
        .map(render(_) + ";")
        .appended {
          if (returnLast)
            "return " + render(e) + ";"
          else
            render(e) + ";"
        }
        .mkString("\n")
    case E.Assign(lhs, rhs)       => render(lhs) + " = " + render(rhs)
    case E.Ident(name)            => s"$$$name"
    case E.Echo(arg)              => "echo " + render(arg)
    case E.StringConcat(lhs, rhs) => s"${render(lhs)} . ${render(rhs)}"
    case E.Addition(lhs, rhs)     => s"${render(lhs)} + ${render(rhs)}"
    case E.FunctionDef(name, argName, body) => s"""|function $name($$$argName) {
                                                   |${render(body, returnLast = true).indentTrim(2)}
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

def stat(
  s: String
) = s + ";\n"

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
