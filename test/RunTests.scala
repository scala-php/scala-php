//> using lib "com.disneystreaming::weaver-cats::0.8.4"
//> using testFramework "weaver.framework.CatsEffect"
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all._
import weaver._

import java.nio.file.Files
import java.util.UUID

object RunTests extends SimpleIOSuite {
  test("literal val") {
    phpRun {
      val x = 42
      println(x)
    }.map(_.stdout.trim)
      .map(assert.same(_, "42"))
  }

  test("identity function") {
    phpRun {
      def id(
        x: Int
      ) = x
      println(id(42))
    }.map(_.stdout.trim)
      .map(assert.same(_, "42"))
  }

  test("function using global") {
    phpRun {
      val x = 42

      def bar(
        i: Int
      ) = x + i

      println(bar(420))
    }.map(_.stdout.trim)
      .map(assert.same(_, "462"))
  }

  test("complex arithmetic") {
    phpRun {
      println(50 + 20 * 100 / 2)
    }.map(_.stdout.trim)
      .map(assert.same(_, "1050"))
  }

  test("conditionals") {
    phpRun {
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

      println(fun(true, false))
      println(fun(false, true))
      println(fun(false, false))
    }.map(_.stdout.trim)
      .map(
        assert.same(
          _,
          """hello
            |goodbye
            |secret third option""".stripMargin,
        )
      )
  }

  test("variable assignment") {
    phpRun {
      var test = 40
      test = 42
      println(test)
    }.map(_.stdout.trim)
      .map(assert.same(_, "42"))
  }

  test("multiple vars") {
    phpRun {
      val x = 42
      val y = 50

      println(x + y)
    }.map(_.stdout.trim)
      .map(assert.same(_, "92"))
  }

  test("concat") {
    phpRun {
      var comma = ""
      val concatDupe = (s: String) => s + comma + s
      comma = ", "

      println(concatDupe("hello"))
    }.map(_.stdout.trim)
      .map(assert.same(_, "hello, hello"))
  }

  test("concatenating simple strings") {
    phpRun {
      val text1 = "Hello"
      val text2 = "World"
      println(text1 + text2)
    }.map(_.stdout.trim)
      .map(assert.same(_, "HelloWorld"))
  }

  test("lambda") {
    phpRun {
      val addOne = (x: Int) => x + 1
      println(addOne(42))
    }.map(_.stdout.trim)
      .map(assert.same(_, "43"))
  }

  test("currying") {
    phpRun {
      val add = (x: Int) => (y: Int) => x + y
      val addOne = add(1)
      println(addOne(42))
    }.map(_.stdout.trim)
      .map(assert.same(_, "43"))
  }

  test("lambdas using globals") {
    phpRun {
      var x = 42
      val add = (y: Int) => x + y
      x = 10
      println(add(420))
    }.map(_.stdout.trim)
      .map(assert.same(_, "430"))
  }

  test("lambda with multiple args") {
    phpRun {
      val add =
        (
          x: Int,
          y: Int,
        ) => x + y
      println(add(1, 20))
    }.map(_.stdout.trim)
      .map(assert.same(_, "21"))
  }

  test("class members") {
    phpRun {

      case class Data(
        s: String,
        i: Int,
        private val x: Int,
      )

      val d = new Data("hello", 42, 0)
      println(d.s + d.i)
    }.map(_.stdout.trim)
      .map(assert.same(_, "hello42"))
  }

  test("class method using private member") {
    phpRun {

      case class Data(
        s: String,
        i: Int,
        private val x: Int,
      ) {
        def printed = s + i + x
      }

      val d = new Data("hello", 42, 52)
      println(d.printed)
    }.map(_.stdout.trim)
      .map(assert.same(_, "hello4252"))
  }

  private def slurp(
    command: List[String]
  ): IO[
    RunResult
  ] = IO.interruptible {
    import scala.jdk.CollectionConverters._
    val process = new ProcessBuilder(command.asJava).start()
    val output = new String(process.getInputStream.readAllBytes())
    val returnCode = process.waitFor()
    RunResult(output, returnCode)
  }

  private inline def phpRun(
    inline code: Any
  ): IO[RunResult] = {
    val testId = UUID.randomUUID().toString()
    val expr = php(code)

    val fileText =
      s"""<?php
         |${renderPublic(expr)}
         |""".stripMargin

    Resource
      .make(IO(Files.createTempFile("scala-php-test", s"$testId.php")))(file =>
        IO(Files.delete(file))
      )
      .use { file =>
        IO(Files.writeString(file, fileText)) *>
          slurp("php" :: file.toString() :: Nil).flatMap { result =>
            if (result.exitCode != 0)
              IO.raiseError(new Exception(s"PHP exited with code ${result.exitCode}"))
            else
              IO.pure(result)
          }
      }
  }

  case class RunResult(
    stdout: String,
    exitCode: Int,
  )

}
