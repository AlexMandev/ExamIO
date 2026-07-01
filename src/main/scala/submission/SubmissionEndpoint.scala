package submission

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.integ.cats.codec.*
import sttp.model.StatusCode.{BadRequest, Conflict, Created, Forbidden, NotFound}

import infrastructure.ExamIOEndpoints.{apiBaseEndpoint, secure}

import exam.{ExamId, ExamDoesNotExist, NotAStudent, ExamCannotBeOpened, ExamError}
import user.UserRole
import utils.jsonBodyTypedError

object SubmissionEndpoint:
  private val submissionsBaseEndpoint = apiBaseEndpoint.in("exams" / path[ExamId]("examId") / "submissions")

  val createSubmissionEndpoint = submissionsBaseEndpoint
    .secure(UserRole.STUDENT,
        oneOf[ExamError | SubmissionError](
          oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
          oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAStudent])),
          oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamCannotBeOpened])),
          oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[AlreadySubmitted]))
        )
      )
    .out(statusCode(Created).and(jsonBody[Submission]))
    .post

  val finishSubmissionEndpoint = submissionsBaseEndpoint
    .in(path[SubmissionId]("submissionId") / "submit")
    .secure(UserRole.STUDENT,
        oneOf[ExamError | SubmissionError](
          oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
          oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAStudent])),
          oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamCannotBeOpened])),
          oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[AlreadySubmitted])),
          oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[SubmissionNotInProgress]))
        )
      )
    .out(statusCode(Created))
    .patch
