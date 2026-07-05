package question

import cats.effect.{IO, Resource}
import infrastructure.auth.AuthenticationService
import infrastructure.db.DBDoobie.DBTransactor
import sttp.tapir.server.ServerEndpoint
import exam.ExamService
import submission.SubmissionService

case class QuestionModule(
  questionRepository: QuestionRepository,
  questionService: QuestionService,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object QuestionModule:
  def apply(
    dbTransactor: DBTransactor,
    examService: ExamService,
    submissionService: SubmissionService,
    authenticationService: AuthenticationService
  ): Resource[IO, QuestionModule] =
    val questionRepository = QuestionRepository(dbTransactor)
    val questionService = QuestionService(questionRepository, submissionService, examService)
    val questionController = QuestionController(questionService, authenticationService)
    Resource.pure(QuestionModule(questionRepository, questionService, questionController.endpoints))
