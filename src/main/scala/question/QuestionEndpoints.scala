package question

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.integ.cats.codec.*
import sttp.model.StatusCode.{BadRequest, Conflict, Created, Forbidden, NotFound}
import infrastructure.ExamIOEndpoints.{apiBaseEndpoint, secure}
import user.UserRole.{STUDENT, TEACHER}
import exam.{ExamDoesNotExist, ExamError, ExamId, ExamNotDraft, ExamStatusMismatch, NotAnOwner}
import submission.{SubmissionId, SubmissionDoesNotExist, NotSubmissionOwner}
import utils.jsonBodyTypedError
import submission.SubmissionError

object QuestionEndpoints:
  private val questionsBaseEndpoint = apiBaseEndpoint
    .in("exams" / path[ExamId]("examId") / "questions")
    .tag("Questions")

  val getExamQuestionsEndpoint = questionsBaseEndpoint
    .secure(
      Some(TEACHER),
      oneOf[ExamError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAnOwner]))
      )
    )
    .out(jsonBody[List[Question]])
    .get

  val getStudentQuestionsEndpoint = questionsBaseEndpoint
    .in(path[SubmissionId]("submissionId"))
    .secure(
      Some(STUDENT),
      oneOf[ExamError | SubmissionError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamStatusMismatch])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[SubmissionDoesNotExist])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotSubmissionOwner]))
      )
    )
    .out(jsonBody[List[PublicQuestion]])
    .get

  val deleteQuestionEndpoint = questionsBaseEndpoint
    .in(path[QuestionId]("questionId"))
    .secure(
      Some(TEACHER),
      oneOf[DeleteQuestionError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAnOwner])),
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamNotDraft])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[QuestionNotFound]))
      )
    )
    .delete

  val addQuestionEndpoint = questionsBaseEndpoint
    .secure(
      Some(TEACHER),
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
