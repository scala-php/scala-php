import java.nio.file.Files
import java.nio.file.Paths

object HelloWorld {

  def main(
    args: Array[String]
  ): Unit = {
    val p = Paths.get("non-existent")
    println(Files.readString(p))
  }

}
