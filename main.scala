//> using scala "3.3.1"
//> using option "-Wunused:all"
//> using option "-no-indent"
//> using lib "com.kubukoz::debug-utils:1.1.3"
//> using lib "com.lihaoyi::pprint:0.8.1"
import java.nio.file.Files
import java.nio.file.Paths

@main def go = {

  // to run in Scala, replace php with identity (or unwrap it entirely)
  val ast: E | Unit = php {
    val x = 42

    def foo(
      i: Int
    ) = i + 1

    def bar(
      i: Int
    ) = foo(x + i - 1)

    def greet(
      s: String
    ) = {
      val y = 50
      println(x)
      println(s"hello, $s $x $y")
      println(50 + 20 * 100 / 2)
      println(foo(bar(y)))
    }

    def fun(
      b: Boolean,
      b2: Boolean,
    ) =
      if (b) {
        val z = "hello"
        z
      } else if (b2)
        "goodbye"
      else
        "secret third option"

    println(bar(420))
    greet("Kuba")

    println(fun(true, false))
    println(fun(false, true))
    println(fun(false, false))

    var test = 40
    println { test = 40 }
    greet {
      "Kuba"
    }
  }

  ast match {
    case e: E =>
      pprint.pprintln(ast)
      val code =
        s"""<?php
           |${renderPublic(e)}
           |""".stripMargin

      Files.writeString(Paths.get("demo.php"), code)

      import sys.process.*
      val returnCode = Process("php" :: "demo.php" :: Nil)
        .!(ProcessLogger(println(_)))
      if (returnCode != 0)
        println(s"error code: $returnCode")
    case _ => ()
  }
}
