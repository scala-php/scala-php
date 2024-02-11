import cats.effect.IO
import cats.syntax.all._
import weaver._

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

  private inline def toCode(
    inline expr: Any
  ): String = renderPublic(php(expr), includePrelude = false)

}
