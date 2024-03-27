lazy val root = (project in file("."))
  .settings(
    scalaVersion := "3.4.0"
  )
  .enablePlugins(PhpPlugin)
