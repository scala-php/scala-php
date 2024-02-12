val Scala3 = "3.3.1"

ThisBuild / organization := "org.scala-php"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / doc / sources := Nil

val plugin = project
  .settings(
    scalaVersion := Scala3,
    name := "scala-php-plugin",
    crossTarget := target.value / s"scala-${scalaVersion.value}", // workaround for https://github.com/sbt/sbt/issues/5097
    crossVersion := CrossVersion.full,
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

val sbtPlugin = project
  .in(file("sbt-plugin"))
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
    publishLocal := {
      val _ = (plugin / Compile / publishLocal).value
      publishLocal.value
    },
  )
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := "org.scalaphp.sbtplugin.buildinfo",
    buildInfoKeys ++= Seq(
      version
    ),
  )

val tests = project
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
  )

//todo: no publish
val root = project
  .in(file("."))
  .aggregate(plugin, sbtPlugin, tests)
