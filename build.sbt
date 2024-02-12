ThisBuild / scalaVersion := "3.3.1"

val plugin = project
  .settings(
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

val tests = project
  .settings(
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
  .aggregate(plugin, tests)
