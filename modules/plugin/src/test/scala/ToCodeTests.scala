import cats.effect.IO
import cats.syntax.all._
import weaver._

import java.nio.file.Files
import java.nio.file.Paths

object TranslateTests extends FunSuite {
  test("literal val") {
    assert.same(
      toCode { val x = 42 },
      """scala_Unit::consume($x = 42)""".stripMargin,
    )
  }

  test("identity function") {
    assert.same(
      toCode {
        def id(
          x: Int
        ) = x
      },
      """scala_Unit::consume(function id($x) {
        |  return $x;
        |})""".stripMargin,
    )
  }

  test("function that captures a global") {
    assert.same(
      toCode {
        val x = 42

        def bar(
          i: Int
        ) = x + i
      },
      """$x = 42;
        |function bar($i) {
        |  global $x;
        |  return $x + $i;
        |}
        |scala_Unit::consume();""".stripMargin,
    )
  }

  test("lambda that captures a global") {
    assert.same(
      toCode {
        val x = 42
        val bar = (i: Int) => x + i
      },
      """$x = 42;
        |$bar = function ($i) use (&$x) {
        |  return $x + $i;
        |};
        |scala_Unit::consume();""".stripMargin,
    )

  }

  test("function in a class that uses a field") {
    assert.same(
      toCode {
        case class C(
          x: Int
        ) {
          def foo: Int = x
        }
      },
      """class C {
        |  public $x;
        |  function __construct($x) {
        |    $this->x = $x;
        |  }
        |  function foo() {
        |    return $this->x;
        |  }
        |};
        |$C = new CDOLLAR();
        |class CDOLLAR {
        |
        |  function __construct() {
        |
        |  }
        |
        |};
        |scala_Unit::consume();""".stripMargin,
    )

  }

  test("function in a class that uses a field") {
    assert.same(
      toCode {
        case class C(
          x: Int
        )

        new C(42).x
      },
      """class C {
        |  public $x;
        |  function __construct($x) {
        |    $this->x = $x;
        |  }
        |
        |};
        |$C = new CDOLLAR();
        |class CDOLLAR {
        |
        |  function __construct() {
        |
        |  }
        |
        |};
        |new C(42)->x;""".stripMargin,
    )
  }

  test("Paths.get: single param") {
    assert.same(
      toCode {
        Paths.get("foo")
      },
      """"foo" . "";""".stripMargin,
    )
  }

  test("Paths.get: many params") {
    assert.same(
      toCode {
        Paths.get("foo", "bar", "baz")
      },
      """"foo" . "" . "/" . "bar" . "/" . "baz";""",
    )
  }

  test("Files.readString") {
    assert.same(
      toCode {
        Files.readString(Paths.get("foo"))
      },
      """java_nio_file_Files_readString("foo" . "");""",
    )
  }

  test("native function") {
    import org.scalaphp.php

    assert.same(
      """
        |explode(" ", "foo bar");""".stripMargin,
      toCode {
        @php.native
        def explode(
          delim: String,
          s: String,
        ): Array[String] = php.native

        explode(s = "foo bar", delim = " ")
      },
    )
  }

  private inline def toCode(
    inline expr: Any
  ): String = renderPublic(toPhp(expr), includePrelude = false)

}
