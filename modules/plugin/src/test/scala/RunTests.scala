import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all._
import weaver._

import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

object RunTests extends SimpleIOSuite {
  test("literal val") {
    phpRun {
      val x = 42
      println(x)
    }.map(_.stdout.trim)
      .map(assert.same("42", _))
  }

  test("identity function") {
    phpRun {
      def id(
        x: Int
      ) = x
      println(id(42))
    }.map(_.stdout.trim)
      .map(assert.same("42", _))
  }

  test("function using global") {
    phpRun {
      val x = 42

      def bar(
        i: Int
      ) = x + i

      println(bar(420))
    }.map(_.stdout.trim)
      .map(assert.same("462", _))
  }

  test("complex arithmetic") {
    phpRun {
      println(50 + 20 * 100 / 2)
    }.map(_.stdout.trim)
      .map(assert.same("1050", _))
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
          """hello
            |goodbye
            |secret third option""".stripMargin,
          _,
        )
      )
  }

  test("variable assignment") {
    phpRun {
      var test = 40
      test = 42
      println(test)
    }.map(_.stdout.trim)
      .map(assert.same("42", _))
  }

  test("multiple vars") {
    phpRun {
      val x = 42
      val y = 50

      println(x + y)
    }.map(_.stdout.trim)
      .map(assert.same("92", _))
  }

  test("concat") {
    phpRun {
      var comma = ""
      val concatDupe = (s: String) => s + comma + s
      comma = ", "

      println(concatDupe("hello"))
    }.map(_.stdout.trim)
      .map(assert.same("hello, hello", _))
  }

  test("concatenating simple strings") {
    phpRun {
      val text1 = "Hello"
      val text2 = "World"
      println(text1 + text2)
    }.map(_.stdout.trim)
      .map(assert.same("HelloWorld", _))
  }

  test("lambda") {
    phpRun {
      val addOne = (x: Int) => x + 1
      println(addOne(42))
    }.map(_.stdout.trim)
      .map(assert.same("43", _))
  }

  test("currying") {
    phpRun {
      val add = (x: Int) => (y: Int) => x + y
      val addOne = add(1)
      println(addOne(42))
    }.map(_.stdout.trim)
      .map(assert.same("43", _))
  }

  test("lambdas using globals") {
    phpRun {
      var x = 42
      val add = (y: Int) => x + y
      x = 10
      println(add(420))
    }.map(_.stdout.trim)
      .map(assert.same("430", _))
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
      .map(assert.same("21", _))
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
      .map(assert.same("hello42", _))
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
      .map(assert.same("hello4252", _))
  }

  test("reading text file") {
    phpRun {
      println(
        Files.readString(Paths.get("modules", "plugin", "src", "test", "resources", "example.txt"))
      )
    }
      .map(_.stdout.trim)
      .map(assert.same("""hello world""", _))
  }

  test("reading text file fails if it doesn't exist") {
    phpRun {
      println(
        Files.readString(Paths.get("doesnt-exist"))
      )
    }
      .attempt
      .map(matches(_) { case Left(_) => success })
  }

  test("native explode function") {
    import org.scalaphp.php
    phpRun {
      @php.native
      def explode(
        delim: String,
        s: String,
      ): Array[String] = php.native

      println(explode(s = "foo bar", delim = " ")(0))
    }
      .map(_.stdout.trim)
      .map(
        assert.same("foo", _)
      )
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

  private object phpRun {

    case class Options(
      debug: Boolean
    ) {
      def withDebug: Options = copy(debug = true)
    }

    object Options {
      val default: Options = Options(debug = false)
    }

    inline def debug(
      inline code: Any
    ): IO[RunResult] = apply(code, Options.default.withDebug)

    inline def apply(
      inline code: Any
    ): IO[RunResult] = run(code, Options.default)

    private inline def run(
      inline code: Any,
      options: Options,
    ): IO[RunResult] = {
      val testId = UUID.randomUUID().toString()
      val expr = toPhp(code)

      val fileText =
        s"""<?php
           |${renderPublic(expr)}
           |""".stripMargin

      Resource
        .make(IO(Files.createTempFile("scala-php-test", s"$testId.php")))(file =>
          {
            IO.println("paused test for: " + file + " - press enter to continue") *>
              IO.readLine
          }.whenA(options.debug) *>
            IO(Files.delete(file))
        )
        .use { file =>
          IO(Files.writeString(file, fileText)) *>
            slurp("php" :: file.toString() :: Nil).flatMap { result =>
              if (result.exitCode != 0)
                IO.raiseError(
                  new Exception(
                    s"PHP exited with code ${result.exitCode}. Output: ${result.stdout}"
                  )
                )
              else
                IO.pure(result)
            }
        }
    }

  }

  case class RunResult(
    stdout: String,
    exitCode: Int,
  )

}
