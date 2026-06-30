package question

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.integ.cats.codec.*
import sttp.model.StatusCode.{BadRequest, Conflict, Created, Forbidden, NotFound}
import infrastructure.ExamIOEndpoints.*
import user.UserRole.TEACHER
import exam.{ExamDoesNotExist, ExamError, ExamId, ExamNotDraft, NotAnOwner}
import infrastructure.auth.AuthenticationError
import utils.jsonBodyTypedError

object QuestionEndpoints:
  private val questionsBaseEndpoint = apiBaseEndpoint
    .in("exams" / path[ExamId]("examId") / "questions")
    .tag("Questions")

  private val teachersBaseSecureEndpoint = questionsBaseEndpoint.secure(TEACHER)

  val getExamQuestionsEndpoint = questionsBaseEndpoint.out(jsonBody[List[Question]]).get

  val addQuestionEndpoint
    : Endpoint[String, (ExamId, QuestionForm), AuthenticationError | ExamError | QuestionError, Question, Any] =
    questionsBaseEndpoint
      .secure(TEACHER)
      .in(jsonBody[QuestionForm])
      .errorOutVariant(
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist]))
      )
      .errorOutVariant(
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAnOwner]))
      )
      .errorOutVariant(
        oneOfVariant(statusCode(BadRequest).and(jsonBodyTypedError[QuestionFormValidationError]))
      )
      .errorOutVariant(
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamNotDraft]))
      )
      .out(statusCode(Created).and(jsonBody[Question]))
      .post
