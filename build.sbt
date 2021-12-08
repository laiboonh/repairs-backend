val Http4sVersion = "0.23.6"
val CirceVersion = "0.14.1"
val ScalaTestVersion = "3.2.10"
val SkunkVersion = "0.2.2"
val TestContainersScalaVersion = "0.39.12"
val CirisVersion = "2.2.1"
val RefinedVersion = "0.9.28"
val Log4CatsVersion = "2.1.1"
val TsecVersion = "0.4.0"
val Slf4jVersion = "2.0.0-alpha5"
val MockitoVersion = "3.2.10.0"
val NewtypeVersion = "0.4.4"
val DerevoVersion = "0.12.8"

lazy val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.13.7",
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
)

lazy val root = project
  .in(file("."))
  .settings(
    commonSettings,
    name := "repairs-backend",
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
      "io.estatico" %% "newtype" % "0.4.4",
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "com.dimafeng" %% "testcontainers-scala-scalatest" % TestContainersScalaVersion % "test",
      "com.dimafeng" %% "testcontainers-scala-postgresql" % TestContainersScalaVersion % "test",
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test,
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-refined" % "1.0.0-RC1",
    ),
    Test / fork := true //https://github.com/testcontainers/testcontainers-scala
  )
  .enablePlugins(JavaAppPackaging) //https://devcenter.heroku.com/articles/deploying-scala
  .aggregate(macros)
  .dependsOn(macros)

lazy val macros = project
  .in(file("macros"))
  .settings(
    commonSettings,
    name := "repairs-backend-models",
    libraryDependencies ++= Seq(
      "io.estatico" %% "newtype" % NewtypeVersion,
      "eu.timepit" %% "refined" % RefinedVersion,
      "io.circe" %% "circe-refined" % CirceVersion,
      "tf.tofu" %% "derevo-circe" % DerevoVersion,
    ),
    scalacOptions ++= Seq(
      "-Ymacro-annotations"
    )
  )

