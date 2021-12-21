val Http4sVersion = "0.23.7"
val CirceVersion = "0.14.1"
val ScalaTestVersion = "3.2.10"
val SkunkVersion = "0.2.2"
val TestContainersScalaVersion = "0.39.12"
val CirisVersion = "2.3.1"
val RefinedVersion = "0.9.28"
val Log4CatsVersion = "2.1.1"
val TsecVersion = "0.4.0"
val Slf4jVersion = "2.0.0-alpha5"
val MockitoVersion = "3.2.10.0"
val NewtypeVersion = "0.4.4"
val DerevoVersion = "0.12.8"
val DoobieVersion = "1.0.0-RC1"

lazy val root = project
  .in(file("."))
  .settings(
    version := "0.1",
    scalaVersion := "2.13.7",
    name := "repairs-backend",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-refined" % CirceVersion,
      "org.tpolecat" %% "skunk-core" % SkunkVersion,
      "is.cir" %% "ciris" % CirisVersion,
      "is.cir" %% "ciris-enumeratum" % CirisVersion,
      "is.cir" %% "ciris-refined" % CirisVersion,
      "eu.timepit" %% "refined-cats" % RefinedVersion,
      "org.typelevel" %% "log4cats-slf4j" % Log4CatsVersion,
      "org.slf4j" % "slf4j-simple" % Slf4jVersion,
      "io.github.jmcardon" %% "tsec-http4s" % TsecVersion,
      "org.tpolecat" %% "doobie-core" % DoobieVersion,
      "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
      "org.tpolecat" %% "doobie-refined" % DoobieVersion,
      "org.tpolecat" %% "doobie-scalatest" % DoobieVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      "com.dimafeng" %% "testcontainers-scala-scalatest" % TestContainersScalaVersion % Test,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % TestContainersScalaVersion % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test,
    ),
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
    Test / fork := true //https://github.com/testcontainers/testcontainers-scala
  )
  .enablePlugins(JavaAppPackaging) //https://devcenter.heroku.com/articles/deploying-scala