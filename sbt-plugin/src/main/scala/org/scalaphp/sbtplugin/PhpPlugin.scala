package org.scalaphp.sbtplugin

import org.scalaphp.sbtplugin.buildinfo.BuildInfo
import sbt._

import Keys._

object PhpPlugin extends AutoPlugin {
  override def trigger = noTrigger

  object autoImport {
    val scalaPhpVersion = settingKey[String]("The version of scala-php in use")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    scalaPhpVersion := BuildInfo.version,
    libraryDependencies ++= Seq(
      compilerPlugin(
        "org.scala-php" % "scala-php-plugin" % scalaPhpVersion.value cross CrossVersion.full
      )
    ),
  )

}
