val Http4sVersion = "0.23.6"
val CirceVersion = "0.14.1"
val ScalaTestVersion = "3.2.10"
val SkunkVersion = "0.2.2"
val TestContainersScalaVersion = "0.39.12"
val CirisVersion = "2.2.1"
val RefinedVersion = "0.9.27"
val Log4CatsVersion = "2.1.1"
val TsecVersion = "0.4.0"
val Slf4jVersion = "2.0.0-alpha5"

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
      "is.cir" %% "ciris" % CirisVersion,
      "is.cir" %% "ciris-enumeratum" % CirisVersion,
      "is.cir" %% "ciris-refined" % CirisVersion,
      "eu.timepit" %% "refined-cats" % RefinedVersion,
      "org.typelevel" %% "log4cats-slf4j" % Log4CatsVersion,
      "org.slf4j" % "slf4j-simple" % Slf4jVersion,
      "io.github.jmcardon" %% "tsec-http4s" % TsecVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "com.dimafeng" %% "testcontainers-scala-scalatest" % TestContainersScalaVersion % "test",
      "com.dimafeng" %% "testcontainers-scala-postgresql" % TestContainersScalaVersion % "test"
    ),
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),

    Test / fork := true //https://github.com/testcontainers/testcontainers-scala
  )
  .enablePlugins(JavaAppPackaging) //https://devcenter.heroku.com/articles/deploying-scala