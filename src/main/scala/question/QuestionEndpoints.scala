package question

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.integ.cats.codec.*
import sttp.model.StatusCode.{BadRequest, Conflict, Created, Forbidden, NotFound}
import infrastructure.ExamIOEndpoints.{apiBaseEndpoint, secure}
import user.UserRole.TEACHER
import exam.{ExamDoesNotExist, ExamError, ExamId, ExamNotDraft, NotAnOwner}
import utils.jsonBodyTypedError

object QuestionEndpoints:
  private val questionsBaseEndpoint = apiBaseEndpoint
    .in("exams" / path[ExamId]("examId") / "questions")
    .tag("Questions")

  val getExamQuestionsEndpoint = questionsBaseEndpoint
    .secure(
      TEACHER,
      oneOf[ExamError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAnOwner]))
      )
    )
    .out(jsonBody[List[Question]])
    .get

  val addQuestionEndpoint = questionsBaseEndpoint
    .secure(
      TEACHER,
      oneOf[AddQuestionError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAnOwner])),
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamNotDraft])),
        oneOfVariant(statusCode(BadRequest).and(jsonBodyTypedError[QuestionFormValidationError]))
      )
    )
    .in(jsonBody[QuestionForm])
    .out(statusCode(Created).and(jsonBody[Question]))
    .post
