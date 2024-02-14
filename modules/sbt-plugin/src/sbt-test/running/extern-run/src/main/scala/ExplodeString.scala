import org.scalaphp.php

import scala.io.StdIn

object ExplodeString {

  @php.native
  def explode(
    delim: String,
    s: String,
  ): Array[String] = php.native

  def main(
    args: Array[String]
  ): Unit = {
    val greeting = "hello world"

    println(explode(" ", greeting)(0))
  }

}
