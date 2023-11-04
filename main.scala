//> using scala "3.3.1"
//> using option "-Wunused:all"
//> using option "-no-indent"
//> using lib "com.kubukoz::debug-utils:1.1.3"

import java.nio.file.Files
import java.nio.file.Paths

@main def go = {

  val code = php {
    val x = 42

    def greet(
      s: String
    ) = {
      val y = 50
      println(s"hello, $s $x $y")
      println(50 + 20 * 100 / 2)
    }

    greet("Kuba")
  }

  Files.writeString(Paths.get("demo.php"), code)
  import sys.process.*

  val returnCode = Process("php" :: "demo.php" :: Nil).!(ProcessLogger(println(_)))
  if (returnCode != 0)
    println(s"error code: $code")
}
