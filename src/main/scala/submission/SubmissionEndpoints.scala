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
  private val submissionBaseEndpoint =
    apiBaseEndpoint.in("exams" / path[ExamId]("examId") / "submissions").tag("Submissions")

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

  val getResultsEndpoint = submissionBaseEndpoint
    .in("results")
    .secure(
      Some(UserRole.TEACHER),
      oneOf[ExamDoesNotExist | NotAnOwner](
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
      oneOf[ExamDoesNotExist | SubmissionDoesNotExist | NotSubmissionOwner](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[SubmissionDoesNotExist])),
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
