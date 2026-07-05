import cats.effect.kernel.Resource
import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{Host, Port, ipv4, port}
import infrastructure.auth.{TokenSignatureService, AuthenticationService}
import infrastructure.config.AppConfig
import infrastructure.db.DBModule
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import scala.concurrent.duration.*
import user.UserModule
import exam.ExamModule
import question.QuestionModule
import submission.SubmissionModule
import answers.AnswerModule
import utils.SchedulerUtils

object ExamIO extends IOApp.Simple:
  val app: Resource[IO, Server] = for

    config <- AppConfig.loadConfig

    dbModule <- DBModule(config.dbConfig)

    tokenSignatureService = TokenSignatureService(config.jwtConfig)
    authenticationService = AuthenticationService(tokenSignatureService)

    userModule <- UserModule(dbModule.dbTransactor, tokenSignatureService, authenticationService)
    examModule <- ExamModule(dbModule.dbTransactor, authenticationService)
    questionModule <- QuestionModule(dbModule.dbTransactor, examModule.examService, authenticationService)
    submissionModule <- SubmissionModule(dbModule.dbTransactor, examModule.examService, authenticationService)
    answerModule <- AnswerModule(dbModule.dbTransactor, questionModule.questionService, submissionModule.submissionService, examModule.examService, authenticationService)

    apiEndpoints = userModule.endpoints ++ examModule.endpoints ++ questionModule.endpoints ++ submissionModule.endpoints ++ answerModule.endpoints

    _ <- SchedulerUtils.scheduleAutoSubmissions(submissionModule.submissionService, 10.seconds)

    docs = SwaggerInterpreter().fromServerEndpoints[IO](apiEndpoints, "ExamIO", "1.0.0")
    examIOHttpApp = Http4sServerInterpreter[IO]().toRoutes(apiEndpoints ::: docs).orNotFound

    server <- EmberServerBuilder
      .default[IO]
      .withPort(Port.fromInt(config.httpConfig.port).getOrElse(port"3000"))
      .withHost(Host.fromString(config.httpConfig.host).getOrElse(ipv4"0.0.0.0"))
      .withHttpApp(examIOHttpApp)
      .build
  yield server

  def run: IO[Unit] =
    app.useForever
