val scala3Version = "3.2.2"

lazy val p1 = (project in file("modules/p1")).settings(
  name := "p1",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := scala3Version,
  libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
)

lazy val root = (project in file("."))
  .aggregate(p1)
