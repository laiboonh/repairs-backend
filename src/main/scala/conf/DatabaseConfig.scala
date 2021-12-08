package conf

import cats.effect.std.Console
import cats.effect.{Concurrent, Resource}
import ciris.refined.refTypeConfigDecoder
import ciris.{ConfigError, ConfigValue, Effect, env}
import eu.timepit.refined.auto._
import eu.timepit.refined.refineV
import eu.timepit.refined.types.net.UserPortNumber
import fs2.io.net.Network
import natchez.Trace.Implicits.noop
import skunk.util.Typer.Strategy
import skunk.{SSL, Session}
import types.RefinedTypes._

case class DatabaseConfig(username: String, password: String, host: Host, port: UserPortNumber, databaseName: String) {
  def skunkSession[F[_] : Concurrent : Network : Console]: Resource[F, Session[F]] = Session.single(
    host = host,
    port = port,
    user = username,
    database = databaseName,
    password = Some(password),
    strategy = Strategy.SearchPath,
    ssl = SSL.Trusted.withFallback(true) //fallback for dev environment with no SSL
  )
}

object DatabaseConfig {
  val config: ConfigValue[Effect, DatabaseConfig] = env("DATABASE_URL").as[DatabaseUrl].flatMap { url =>
    DatabaseConfig(url) match {
      case Left(s) => ConfigValue.failed(ConfigError(s))
      case Right(v) => ConfigValue.default(v)
    }
  }

  private def apply(databaseUrl: DatabaseUrl): Either[String, DatabaseConfig] = {
    val pattern = Predef.augmentString("^(.+):/{2}(.+):(.+)@(.+):(.+)/(.+)$").r
    val pattern(_, username, password, host, port, dbName) = databaseUrl.value

    for {
      refinedHost <- refineV(host): Either[String, Host]
      portInt <- try Right(Integer.parseInt(port)) catch {
        case e: Exception => Left(e.getMessage)
      }
      refinedPort <- refineV(portInt): Either[String, UserPortNumber]
    } yield DatabaseConfig(
      username, password, refinedHost, refinedPort, dbName
    )
  }
}