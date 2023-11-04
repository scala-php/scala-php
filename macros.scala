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

  val code: String = transpile(e.asTerm)

  s"""<?php
     |$code
     |""".stripMargin
}

def transpile(
  using q: Quotes
)(
  e: q.reflect.Tree
): String = {
  import quotes.reflect.*

  e match {
    case Inlined(_, _, e)   => transpile(e)
    case Block(stats, expr) => stats.map(transpile(_)).mkString("\n") + "\n" + transpile(expr)
    case Ident(s)           => "$" + s
    case Apply(Select(e1, "+"), List(e2)) =>
      if (e1.tpe <:< TypeRepr.of[String])
        transpile(e1) + " . " + transpile(e2)
      else if (e1.tpe <:< TypeRepr.of[Int])
        transpile(e1) + " + " + transpile(e2)
      else
        report.errorAndAbort("couldn't concat values of type " + e1.tpe)

    case Apply(Ident("println"), Nil)       => s"""echo "\\n";"""
    case Apply(Ident("println"), List(msg)) => s"""echo ${transpile(msg)} . "\\n";"""
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
      val partsEscaped = constantParts
        .map { case Literal(StringConstant(s)) => s }
        .map(StringContext.processEscapes(_))

      val argz = args.map(transpile(_))

      '"' +
        partsEscaped.head +
        argz.zip(partsEscaped.tail).map { case (arg, part) => arg + part }.mkString +
        '"'
    case Assign(lhs, rhs)           => s"${transpile(lhs)} = ${transpile(rhs)};"
    case Literal(UnitConstant())    => ""
    case Literal(StringConstant(v)) => '"' + v + '"'
    case Literal(IntConstant(v))    => v.toString
    case ValDef(name, _, Some(v))   => s"$$$name = ${transpile(v)};"
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
