import scala.io.StdIn

object Demo {

  def main(
    args: Array[String]
  ): Unit = {
    val greeting = "hello world!"

    val in = StdIn.readLine()

    println(s"$greeting, $in. Testing the compiler plugin. this is crazy!")
  }

}
