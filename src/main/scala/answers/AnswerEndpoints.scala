package answers

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.integ.cats.codec.*
import sttp.model.StatusCode
import sttp.model.StatusCode.{Created, NotFound, BadRequest, Conflict, Forbidden}

import infrastructure.ExamIOEndpoints.{secure, apiBaseEndpoint}

import utils.jsonBodyTypedError
import user.UserRole
import question.{QuestionId, QuestionNotFound}
import exam.{ExamId, ExamDoesNotExist, ExamStatusMismatch, NotAnOwner}
import submission.{SubmissionId, SubmissionDoesNotExist, AlreadySubmitted, NotSubmissionOwner, SubmissionNotInExam}

object AnswerEndpoints:
  private val answerBaseEndpoint = apiBaseEndpoint
    .in("exams" / path[ExamId]("examId"))
    .in("submissions" / path[SubmissionId]("submissionId"))
    .in("answers" / path[QuestionId]("questionId"))
    .tag("Answers")

  def getAnswerEndpoint = answerBaseEndpoint
    .secure(
      None,
      oneOf[AnswerServiceError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[QuestionNotFound])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[SubmissionDoesNotExist])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[SubmissionNotInExam])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotSubmissionOwner])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAnOwner])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[AnswerDoesNotExist]))
      )
    )
    .out(jsonBody[Answer])
    .get

  def addAnswerEndpoint = answerBaseEndpoint
    .secure(
      Option(UserRole.STUDENT),
      oneOf[AnswerServiceError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[QuestionNotFound])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[SubmissionDoesNotExist])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[SubmissionNotInExam])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotSubmissionOwner])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[AnswerDoesNotExist])),
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamStatusMismatch])),
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[AlreadySubmitted])),
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[AnswerTypeMismatch])),
        oneOfVariant(statusCode(BadRequest).and(jsonBodyTypedError[AnswerFormValidationError]))
      )
    )
    .in(jsonBody[AnswerForm])
    .out(statusCode(Created).and(jsonBody[Answer]))
    .post

  def clearAnswerEndpoint = answerBaseEndpoint
    .secure(
      Some(UserRole.STUDENT),
      oneOf[AnswerServiceError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[QuestionNotFound])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[SubmissionDoesNotExist])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[SubmissionNotInExam])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotSubmissionOwner])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[AnswerDoesNotExist]))
      )
    )
    .delete

  def gradeAnswerEndpoint = answerBaseEndpoint
    .in("grade")
    .secure(
      Some(UserRole.TEACHER),
      oneOf[ExamDoesNotExist | NotAnOwner | ExamStatusMismatch | QuestionNotFound | AnswerDoesNotExist | InvalidPointsAwarded](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAnOwner])),
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamStatusMismatch])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[QuestionNotFound])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[AnswerDoesNotExist])),
        oneOfVariant(statusCode(BadRequest).and(jsonBodyTypedError[InvalidPointsAwarded]))
      )
    )
    .in(jsonBody[GradeAnswerForm])
    .out(jsonBody[Answer])
    .patch
