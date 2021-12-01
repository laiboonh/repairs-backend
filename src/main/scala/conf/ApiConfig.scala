package conf

import ciris.refined._
import ciris.{ConfigValue, Effect, env}
import eu.timepit.refined.auto._
import eu.timepit.refined.types.net.NonSystemPortNumber

case class ApiConfig(port: NonSystemPortNumber)

object ApiConfig {
  val config: ConfigValue[Effect, ApiConfig] = env("PORT").as[NonSystemPortNumber].default(8080).map(ApiConfig(_))
}
