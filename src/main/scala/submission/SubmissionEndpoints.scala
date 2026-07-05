package submission

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.integ.cats.codec.*
import sttp.model.StatusCode.{BadRequest, Conflict, Created, Forbidden, NotFound, Ok}

import infrastructure.ExamIOEndpoints.{apiBaseEndpoint, secure}

import exam.{ExamId, ExamDoesNotExist, ExamStatusMismatch, ExamError, NotAnOwner}
import user.UserRole
import utils.jsonBodyTypedError

object SubmissionEndpoints:
  private val examBaseEndpoint = apiBaseEndpoint.in("exams" / path[ExamId]("examId")).tag("Submissions")

  private val submissionBaseEndpoint = examBaseEndpoint.in("submissions")

  private val submissionEndpoint = submissionBaseEndpoint.in(path[SubmissionId]("submissionId") / "submit")

  val createSubmissionEndpoint = submissionBaseEndpoint
    .secure(
      Some(UserRole.STUDENT),
      oneOf[ExamError | SubmissionError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamStatusMismatch])),
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[SubmissionAlreadyExists]))
      )
    )
    .out(statusCode(Created).and(jsonBody[Submission]))
    .post

  val getResultsEndpoint = examBaseEndpoint
    .in("results")
    .secure(
      Some(UserRole.TEACHER),
      oneOf[ExamError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAnOwner]))
      )
    )
    .out(jsonBody[List[Submission]])
    .get

  val getResultEndpoint = submissionBaseEndpoint
    .in(path[SubmissionId]("submissionId") / "result")
    .secure(
      Some(UserRole.STUDENT),
      oneOf[ExamError | SubmissionError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[SubmissionDoesNotExist])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[SubmissionNotInExam])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotSubmissionOwner]))
      )
    )
    .out(jsonBody[Submission])
    .get

  val finishSubmissionEndpoint = submissionEndpoint
    .secure(
      Some(UserRole.STUDENT),
      oneOf[ExamError | SubmissionError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[SubmissionDoesNotExist])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotSubmissionOwner])),
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamStatusMismatch])),
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[SubmissionNotInProgress]))
      )
    )
    .out(statusCode(Ok).and(jsonBody[Submission]))
    .patch
