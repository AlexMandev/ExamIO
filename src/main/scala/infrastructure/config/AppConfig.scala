package infrastructure.config

import cats.effect.IO
import cats.effect.kernel.Resource
import com.typesafe.config.{Config, ConfigFactory}
import infrastructure.db.DBConfig
import infrastructure.http.HttpConfig

case class ExamIOConfig(dbConfig: DBConfig, httpConfig: HttpConfig, jwtConfig: JwtConfig)

object AppConfig:
  def loadConfig: Resource[IO, ExamIOConfig] =
    Resource.eval(IO.blocking(ConfigFactory.load())).map(_.getConfig("examio")).map(fromConfig)

  private def fromConfig(config: Config): ExamIOConfig =
    val dbConfig = DBConfig(
      config.getString("db.host"),
      config.getInt("db.port"),
      config.getString("db.name"),
      config.getString("db.user"),
      config.getString("db.password"),
      config.getInt("db.connectionPoolSize"),
      config.getString("db.migrationTable")
    )

    val httpConfig = HttpConfig(config.getString("http.host"), config.getInt("http.port"))
    val jwtConfig = JwtConfig(config.getString("jwt.secret"), config.getLong("jwt.expirationSeconds"))

    ExamIOConfig(dbConfig, httpConfig, jwtConfig)
