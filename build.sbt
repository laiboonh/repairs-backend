val Http4sVersion = "1.0.0-M29"
val CirceVersion = "0.14.1"
val ScalaTestVersion = "3.2.10"
val SkunkVersion = "0.2.2"
val TestContainersScalaVersion = "0.39.12"

lazy val root = project
  .in(file("."))
  .settings(
    name := "repairs-backend",
    version := "0.1.0",

    scalaVersion := "2.13.7",

    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "org.tpolecat" %% "skunk-core" % SkunkVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "com.dimafeng" %% "testcontainers-scala-scalatest" % TestContainersScalaVersion % "test",
      "com.dimafeng" %% "testcontainers-scala-postgresql" % TestContainersScalaVersion % "test"
    ),

    Test / fork := true //https://github.com/testcontainers/testcontainers-scala
  )

