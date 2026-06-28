package exam

import cats.effect.{IO, Resource}
import infrastructure.auth.AuthenticationService
import sttp.tapir.server.ServerEndpoint
import infrastructure.db.DBDoobie.DBTransactor

case class ExamModule(
  examRepository: ExamRepository,
  examService: ExamService,
  authenticationService: AuthenticationService,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object ExamModule:
  def apply(dbTransactor: DBTransactor, authenticationService: AuthenticationService): Resource[IO, ExamModule] =
    val examRepository = ExamRepository(dbTransactor)
    val examService = ExamService(examRepository)
    val examController = ExamController(examService, authenticationService)

    Resource.pure(ExamModule(examRepository, examService, authenticationService, examController.endpoints))
