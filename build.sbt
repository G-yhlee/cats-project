// .settings 대체
// version := "0.1.0-SNAPSHOT",
// scalaVersion := scala3Version,

ThisBuild / scalaVersion := V.Scala
ThisBuild / version := "0.0.1-SNAPSHOT"

val V = new {
  val Scala = "3.3.0-RC4"
}

val Dependencies = new {
  lazy val p1_plain = Seq(
    libraryDependencies ++= Seq(
    )
  )
  lazy val p2_cats = Seq(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.5.0"
    )
  )
}

lazy val p1_plain = (project in file("modules/p1_plain"))
  .settings(
    name := "p1_plain",
    Dependencies.p1_plain
  )
lazy val p2_cats = (project in file("modules/p2_cats"))
  .settings(
    name := "p2_cats",
    Dependencies.p2_cats
  )

lazy val root = (project in file("."))
  .aggregate(
    p1_plain,
    p2_cats
  )
