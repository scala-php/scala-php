package org.scalaphp.sbtplugin

import org.scalaphp.sbtplugin.buildinfo.BuildInfo
import sbt._

import Keys._

object PhpPlugin extends AutoPlugin {
  override def trigger = noTrigger

  object autoImport {
    val scalaPhpVersion = settingKey[String]("The version of scala-php in use")

    val phpBinary = settingKey[Option[File]](
      "The path to the PHP interpreter binary. By default, `php` is used if it's in the PATH."
    )

  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    // default settings
    scalaPhpVersion := BuildInfo.version,
    phpBinary := None,
    // plugin & runtime
    libraryDependencies ++= Seq(
      "org.scala-php" %% "phplib" % scalaPhpVersion.value,
      compilerPlugin(
        "org.scala-php" % "scala-php-plugin" % scalaPhpVersion.value cross CrossVersion.full
      ),
    ),
    // runtime
    run := {
      val compiled = (Compile / compile).value

      import sys.process._

      val logger = (Compile / streams).value.log

      val filesToRun = PathFinder(target.value).**("*.php").get.toList

      logger.debug("Running PHP files: " + filesToRun.mkString(", "))

      (phpBinary.value.fold("php")(_.toString) :: filesToRun.map(_.toString()))
        .lineStream(logger)
        .foreach(println)
    },
  )

}
