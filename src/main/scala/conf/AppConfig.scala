package conf

import cats.implicits._
import ciris.{ConfigValue, Effect}

case class AppConfig(apiConfig: ApiConfig, databaseConfig: DatabaseConfig)

object AppConfig {
  def config: ConfigValue[Effect, AppConfig] = (ApiConfig.config, DatabaseConfig.config).parMapN { (api, database) =>
    AppConfig(api, database)
  }
}



