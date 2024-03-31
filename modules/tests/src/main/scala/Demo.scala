import java.nio.file.Files
import java.nio.file.Paths
import scala.io.StdIn

object Demo {

  def main(
    args: Array[String]
  ): Unit = {

    println("What's your name?")
    val name = StdIn.readLine()

    val greeting = s"hello $name!"

    Files.writeString(Paths.get("greeting.txt"), greeting)

    println(Files.readString(Paths.get("greeting.txt")))
  }

}
