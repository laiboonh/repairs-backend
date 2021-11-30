import cats.effect.{Concurrent, Resource}
import cats.effect.std.Console
import cats.implicits._
import ciris.refined.refTypeConfigDecoder
import ciris.{ConfigError, ConfigValue, Effect, env}
import eu.timepit.refined.refineV
import eu.timepit.refined.types.net.{NonSystemPortNumber, UserPortNumber}
import utils.types.{Host, _}
import eu.timepit.refined.auto._
import fs2.io.net.Network
import natchez.Trace.Implicits.noop
import skunk.{SSL, Session}

case class AppConfig(apiConfig: ApiConfig, databaseConfig: DatabaseConfig)

object AppConfig {
  def config: ConfigValue[Effect, AppConfig] = (ApiConfig.config, DatabaseConfig.config).parMapN { (api, database) =>
    AppConfig(api, database)
  }
}

case class ApiConfig(port: NonSystemPortNumber)

object ApiConfig {
  val config: ConfigValue[Effect, ApiConfig] = env("PORT").as[NonSystemPortNumber].default(8080).map(ApiConfig(_))
}

case class DatabaseConfig(username: String, password: String, host: Host, port: UserPortNumber, databaseName: String) {
  def skunkSession[F[_] : Concurrent : Network : Console]: Resource[F, Session[F]] = Session.single(
    host = host,
    port = port,
    user = username,
    database = databaseName,
    password = Some(password),
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