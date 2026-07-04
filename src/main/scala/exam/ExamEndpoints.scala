package exam

import infrastructure.ExamIOEndpoints.apiBaseEndpoint
import infrastructure.ExamIOEndpoints.*
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody
import sttp.model.StatusCode.{BadRequest, Conflict, Created, Forbidden, NotFound}
import utils.jsonBodyTypedError
import user.UserRole

object ExamEndpoints:
  private val baseExamEndpoint = apiBaseEndpoint.in("exams").tag("Exams")

  val createExamEndpoint = baseExamEndpoint
    .secure(UserRole.TEACHER, oneOf[ExamFormValidationError](
      oneOfVariant(statusCode(BadRequest).and(jsonBodyTypedError[ExamFormValidationError]))
    ))
    .in(jsonBody[ExamForm])
    .out(statusCode(Created).and(jsonBody[Exam]))
    .post

  val getOwnExamsEndpoint = baseExamEndpoint
    .secure(UserRole.TEACHER)
    .out(jsonBody[List[Exam]])
    .get

  val openExamEndpoint = baseExamEndpoint
    .secure(
      UserRole.TEACHER,
      oneOf[OpenExamError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAnOwner])),
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamStatusMismatch]))
      )
    )
    .in(path[ExamId]("examId"))
    .in("open")
    .post

  val closeExamEndpoint = baseExamEndpoint
    .secure(
      UserRole.TEACHER,
      oneOf[CloseExamError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
        oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAnOwner])),
        oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamStatusMismatch]))
      )
    )
    .in(path[ExamId]("examId"))
    .in("close")
    .post
