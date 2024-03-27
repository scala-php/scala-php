ThisBuild / tlBaseVersion := "0.1"

ThisBuild / organization := "org.scala-php"
ThisBuild / organizationName := "Scala.php"
ThisBuild / startYear := Some(2024)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers ++= List(
  tlGitHubDev("kubukoz", "Jakub Kozłowski")
)

ThisBuild / tlSonatypeUseLegacyHost := false
ThisBuild / doc / sources := Nil

val Scala3 = "3.3.3"
val Scala3Versions = Seq("3.3.0", "3.3.1", "3.3.2", Scala3, "3.4.0")

lazy val plugin = project
  .in(file("modules") / "plugin")
  .settings(
    scalaVersion := Scala3,
    crossVersion := CrossVersion.full,
    crossScalaVersions := Scala3Versions,
    name := "scala-php-plugin",
    crossTarget := target.value / s"scala-${scalaVersion.value}", // workaround for https://github.com/sbt/sbt/issues/5097
    libraryDependencies ++= Seq(
      scalaOrganization.value %
        "scala3-compiler_3"
        % scalaVersion.value
    ),
    scalacOptions ++= Seq(
      "-Wunused:all",
      "-no-indent",
    ),
    libraryDependencies ++= Seq(
      "com.kubukoz" %% "debug-utils" % "1.1.3",
      "com.lihaoyi" %% "pprint" % "0.8.1",
      "com.disneystreaming" %% "weaver-cats" % "0.8.4" % Test,
    ),
  )
  .dependsOn(phplib % "test->compile")

lazy val sbtPlugin = project
  .in(file("modules") / "sbt-plugin")
  .settings(
    scalaVersion := "2.12.18",
    name := "scala-php-sbt",
  )
  .enablePlugins(SbtPlugin)
  .settings(
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.9.8"
      }
    },
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
  )
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := "org.scalaphp.sbtplugin.buildinfo",
    buildInfoKeys ++= Seq(
      version
    ),
  )

lazy val phplib = project
  .in(file("modules") / "phplib")
  .settings(
    scalaVersion := Scala3
  )

lazy val tests = project
  .in(file("modules") / "tests")
  .settings(
    scalaVersion := Scala3,
    scalacOptions ++= {
      val jar = (plugin / Compile / packageBin).value
      Seq(
        s"-Xplugin:${jar.getAbsolutePath}",
        s"-Xplugin-require:scala-php",
        s"-Jdummy=${jar.lastModified}",
      ) // borrowed from bm4
    },
    Compile / doc / sources := Seq(),
    run := {
      val compiled = (Compile / compile).value

      import sys.process._

      val logger = (Compile / streams).value.log

      val filesToRun = PathFinder(target.value).**("*.php").get.toList

      logger.debug("Running PHP files: " + filesToRun.mkString(", "))

      ("php" :: filesToRun.map(_.toString()))
        .lineStream(logger)
        .foreach(println)
    },
  )
  .dependsOn(phplib)

//todo: no publish
val root = project
  .in(file("."))
  .aggregate(plugin, sbtPlugin, phplib, tests)
  .settings(
    addCommandAlias("scriptedFull", "+publishLocal;scripted")
  )
