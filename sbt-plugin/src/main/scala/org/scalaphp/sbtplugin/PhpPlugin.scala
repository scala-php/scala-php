package org.scalaphp.sbtplugin

import org.scalaphp.sbtplugin.buildinfo.BuildInfo
import sbt._

import Keys._

object PhpPlugin extends AutoPlugin {
  override def trigger = noTrigger

  object autoImport {
    val scalaPhpVersion = settingKey[String]("The version of scala-php in use")
    val phpBinary = settingKey[File]("The path to the PHP interpreter binary")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    scalaPhpVersion := BuildInfo.version,
    phpBinary := {
      import sys.process._
      file(("which" :: "php" :: Nil).!!.trim)
    },
    libraryDependencies ++= Seq(
      compilerPlugin(
        "org.scala-php" % "scala-php-plugin" % scalaPhpVersion.value cross CrossVersion.full
      )
    ),
    run := {
      val compiled = (Compile / compile).value
      import sys.process._

      val logger = (Compile / streams).value.log

      (phpBinary.value.toString :: "demo.php" :: Nil)
        .lineStream(logger)
        .foreach(println)
    },
  )

}
