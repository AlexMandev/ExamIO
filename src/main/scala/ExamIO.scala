import cats.effect.kernel.Resource
import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{Port, Host, ipv4, port}
import infrastructure.ExamIOEndpoints
import infrastructure.config.AppConfig
import infrastructure.db.DBModule
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import sttp.tapir.server.http4s.Http4sServerInterpreter

object ExamIO extends IOApp.Simple:
  val app: Resource[IO, Server] = for

    config <- AppConfig.loadConfig

    dbModule <- DBModule(config.dbConfig)

    apiEndpoints = ExamIOEndpoints.exampleEndpoints
    examIOHttpApp = Http4sServerInterpreter[IO]().toRoutes(apiEndpoints).orNotFound

    server <- EmberServerBuilder
      .default[IO]
      .withPort(Port.fromInt(config.httpConfig.port).getOrElse(port"3000"))
      .withHost(Host.fromString(config.httpConfig.host).getOrElse(ipv4"0.0.0.0"))
      .withHttpApp(examIOHttpApp)
      .build
  yield server

  def run: IO[Unit] =
    app.useForever
