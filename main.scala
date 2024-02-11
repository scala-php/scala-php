//> using scala "3.3.1"

//> using option "-Wunused:all"
//> using option "-no-indent"
//> using lib "com.kubukoz::debug-utils:1.1.3"
//> using lib "com.lihaoyi::pprint:0.8.1"
import java.nio.file.Files
import java.nio.file.Paths

@main def go = {
  import com.kubukoz.DebugUtils

  // to run in Scala, replace php with identity (or unwrap it entirely)
  val ast: E | Unit = php {

    case class Data(
      s: String,
      i: Int,
      private val x: Int,
    ) {
      def printed = s + i + x
    }

    val d = new Data("hello", 42, 52)
    println(d.printed)
  }

  ast match {
    case e: E =>
      // pprint.pprintln(ast)
      val code =
        s"""<?php
           |${renderPublic(e)}
           |""".stripMargin

      Files.writeString(Paths.get("demo.php"), code)

      import sys.process.*
      val process = Process("php" :: "demo.php" :: Nil)
        .run(true)
      val returnCode = process.exitValue()
      if (returnCode != 0) {
        println(s"error code: $returnCode")
        sys.exit(returnCode)
      }
    case _ => ()
  }
}
