package conf

import cats.implicits.catsSyntaxTuple2Parallel
import ciris.refined._
import ciris.{ConfigValue, Effect, env}
import eu.timepit.refined.auto._
import eu.timepit.refined.types.net.NonSystemPortNumber

import scala.concurrent.duration._
import scala.language.postfixOps

case class ApiConfig(port: NonSystemPortNumber, tokenExpirationTime: FiniteDuration)

object ApiConfig {
  val portConfig: ConfigValue[Effect, NonSystemPortNumber] = env("PORT").as[NonSystemPortNumber].default(8080)
  val tokenExpTimeConfig: ConfigValue[Effect, FiniteDuration] = env("TOKEN_EXP_TIME").as[FiniteDuration].default(10 minutes)
  val config: ConfigValue[Effect, ApiConfig] = (portConfig, tokenExpTimeConfig).parMapN { (port, tokenExpTime) =>
    ApiConfig(port, tokenExpTime)
  }
}
