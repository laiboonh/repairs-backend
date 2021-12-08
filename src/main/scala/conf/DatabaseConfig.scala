package conf

import cats.effect.kernel.Async
import ciris.refined.refTypeConfigDecoder
import ciris.{ConfigError, ConfigValue, Effect, env}
import doobie.Transactor
import doobie.util.transactor.Transactor.Aux
import eu.timepit.refined.refineV
import eu.timepit.refined.types.net.UserPortNumber
import types.RefinedTypes._

case class DatabaseConfig(username: String, password: String, host: Host, port: UserPortNumber, databaseName: String) {
  def doobieTransactor[F[_] : Async]: Aux[F, Unit] = Transactor.fromDriverManager[F](
    driver = "org.postgresql.Driver",
    url = s"jdbc:postgresql://$host:$port/$databaseName",
    user = username,
    pass = password
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