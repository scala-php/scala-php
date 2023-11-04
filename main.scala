//> using scala "3.3.1"
//> using option "-Wunused:all"
//> using option "-no-indent"
//> using lib "com.kubukoz::debug-utils:1.1.3"

import java.nio.file.Files
import java.nio.file.Paths

@main def go = {

  val code = php {
    val greeting = "hello"

    var name = "Kuba"

    def modify(
      s: String
    ) = {
      val a = 42
      s + " " + a + "!"
    }

    println(greeting + ", " + modify(name))
    println()
    name = "Test"
    println(s"$greeting, $name")
  }

  Files.writeString(Paths.get("demo.php"), code)
  import sys.process.*

  val returnCode = Process("php" :: "demo.php" :: Nil).!(ProcessLogger(println(_)))
  if (returnCode != 0)
    println(s"error code: $code")
}
