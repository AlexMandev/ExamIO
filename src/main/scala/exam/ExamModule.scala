package exam

import cats.effect.{IO, Resource}
import infrastructure.auth.AuthenticationService
import sttp.tapir.server.ServerEndpoint
import infrastructure.db.DBDoobie.DBTransactor
import answers.AnswerRepository
import grading.GradingService
import question.QuestionRepository
import submission.SubmissionRepository

case class ExamModule(
  examRepository: ExamRepository,
  examService: ExamService,
  gradingService: GradingService,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object ExamModule:
  def apply(dbTransactor: DBTransactor, authenticationService: AuthenticationService): Resource[IO, ExamModule] =
    val examRepository = ExamRepository(dbTransactor)
    val examService = ExamService(examRepository)
    val gradingService = GradingService(
      examRepository,
      QuestionRepository(dbTransactor),
      AnswerRepository(dbTransactor),
      SubmissionRepository(dbTransactor)
    )
    val examController = ExamController(examService, gradingService, authenticationService)

    Resource.pure(ExamModule(examRepository, examService, gradingService, examController.endpoints))
