//> using scala "3.3.1"

//> using option "-Wunused:all"
//> using option "-no-indent"
//> using lib "com.kubukoz::debug-utils:1.1.3"
//> using lib "com.lihaoyi::pprint:0.8.1"
import java.nio.file.Files
import java.nio.file.Paths
import scala.io.StdIn

@main def go = {
  import com.kubukoz.DebugUtils

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
      println(foo(bar(y)))
    }

    println(50 + 20 * 100 / 2)

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
    test = 42
    println { test = 520 }
    println { val test2 = 520 }
    println("Demo")
    println {
      "Kuba"
    }
    greet {
      "Kuba"
    }
    var comma = ""
    val concatDupe = (s: String) => s + comma + s
    comma = ", "

    println(concatDupe("hello"))

    println("What's your name?")
    // val name = StdIn.readLine()
    // println(s"Hello, $name!")

    val semi: String => String = _ + ";"

    println(semi("hello"))

    val add: (
      Int,
      Int,
    ) => Int = _ + _

    println(add(1, 20))

    val addCurried: Int => Int => Int = a => b => a + b

    println(addCurried(1)(20))
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
      val process = Process("php" :: "demo.php" :: Nil)
        .run(true)
      val returnCode = process.exitValue()
      if (returnCode != 0) {
        println(s"error code: $returnCode")
        sys.exit(returnCode)
      }
    case _ => ()
  }
}
