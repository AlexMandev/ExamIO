package answers

import cats.effect.{IO, Resource}
import infrastructure.auth.AuthenticationService
import infrastructure.db.DBDoobie.DBTransactor
import sttp.tapir.server.ServerEndpoint

import question.QuestionService
import submission.SubmissionService
import exam.ExamService
import grading.GradingService

case class AnswerModule(
  answerRepository: AnswerRepository,
  answerService: AnswerService,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object AnswerModule:
  def apply(
    dbTransactor: DBTransactor,
    questionService: QuestionService,
    submissionService: SubmissionService,
    examService: ExamService,
    gradingService: GradingService,
    authenticationService: AuthenticationService
  ): Resource[IO, AnswerModule] =
    val answerRepository = AnswerRepository(dbTransactor)
    val answerService = AnswerService(answerRepository, questionService, submissionService, examService)
    val answerController = AnswerController(answerService, gradingService, authenticationService)
    Resource.pure(AnswerModule(answerRepository, answerService, answerController.endpoints))
