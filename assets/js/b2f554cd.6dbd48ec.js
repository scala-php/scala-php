"use strict";(self.webpackChunksite_new=self.webpackChunksite_new||[]).push([[880],{8256:e=>{e.exports=JSON.parse('{"blogPosts":[{"id":"010-release","metadata":{"permalink":"/blog/010-release","editUrl":"https://github.com/scala-php/scala-php/tree/main/modules/site/blog/2024-04-01-010-release.md","source":"@site/blog/2024-04-01-010-release.md","title":"Scala.php v0.1.0","description":"Scala.php 0.1.0 has been released!","date":"2024-04-01T00:00:00.000Z","formattedDate":"April 1, 2024","tags":[{"label":"scala-php","permalink":"/blog/tags/scala-php"},{"label":"release-notes","permalink":"/blog/tags/release-notes"}],"readingTime":0.62,"hasTruncateMarker":false,"authors":[{"name":"Jakub Koz\u0142owski","title":"Creator of Scala.php","url":"https://github.com/kubukoz","imageURL":"https://github.com/kubukoz.png","key":"kubukoz"}],"frontMatter":{"slug":"010-release","title":"Scala.php v0.1.0","authors":["kubukoz"],"tags":["scala-php","release-notes"]},"unlisted":false},"content":"Scala.php 0.1.0 has been released!\\n\\nThis is the first public release of Scala.php.\\n\\nScala.php is a PHP backend for the Scala compiler: it takes your Scala code and outputs PHP code from it,\\nwhich you can include into an existing PHP application, run on its own, or package into a library.\\n\\nYou can learn more about its features on [the homepage](/), or you can go ahead and [get started](/docs/getting-started).\\n\\nIn the meantime, here\'s a demo:\\n\\n```scala title=\\"src/main/scala/Demo.scala\\"\\nimport java.nio.file.Files\\nimport java.nio.file.Paths\\nimport scala.io.StdIn\\n\\nobject Demo {\\n\\n  def main(\\n    args: Array[String]\\n  ): Unit = {\\n\\n    println(\\"What\'s your name?\\")\\n    val name = StdIn.readLine()\\n\\n    val greeting = s\\"Hello $name!\\"\\n\\n    Files.writeString(Paths.get(\\"greeting.txt\\"), greeting)\\n\\n    println(Files.readString(Paths.get(\\"greeting.txt\\")))\\n  }\\n\\n}\\n```\\n\\n```plaintext title=\\"Output\\"\\n> What\'s your name?\\n< Jakub\\n> Hello Jakub!\\n```"}]}')}}]);