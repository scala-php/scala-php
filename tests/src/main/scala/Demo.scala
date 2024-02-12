import scala.io.StdIn

object Demo {

  def main(
    args: Array[String]
  ): Unit = {
    def greet(
    ) = {
      val greeting = "hello world!"

      val in = StdIn.readLine()

      s"$greeting, $in. Testing the compiler plugin. this is crazy!"
    }

    println(greet())
  }

}
