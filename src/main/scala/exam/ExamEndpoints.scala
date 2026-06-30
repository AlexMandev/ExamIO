package exam

import infrastructure.ExamIOEndpoints.apiBaseEndpoint
import sttp.tapir.generic.auto.*
import infrastructure.ExamIOEndpoints.*
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody
import sttp.model.StatusCode.{BadRequest, Conflict, Created, Forbidden, NotFound}
import utils.jsonBodyTypedError
import sttp.tapir.integ.cats.codec.*
import user.UserRole

object ExamEndpoints:
  private val baseExamEndpoint = apiBaseEndpoint.in("exams").tag("Exams")

  val createExamEndpoint = baseExamEndpoint
    .secure(UserRole.TEACHER)
    .in(jsonBody[ExamForm])
    .errorOutVariant(oneOfVariant(statusCode(BadRequest).and(jsonBody[ExamCreationError])))
    .out(statusCode(Created).and(jsonBody[Exam]))
    .post

  val getOwnExamsEndpoint = baseExamEndpoint
    .secure(UserRole.TEACHER)
    .out(jsonBody[List[Exam]])
    .get

  val openExamEndpoint = baseExamEndpoint
    .secure(UserRole.TEACHER, oneOf[OpenExamError](
      oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
      oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAnOwner])),
      oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamCannotBeOpened]))
    ))
    .in(path[ExamId]("examId"))
    .in("open")
    .post

  val closeExamEndpoint = baseExamEndpoint
    .secure(UserRole.TEACHER, oneOf[CloseExamError](
      oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[ExamDoesNotExist])),
      oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[NotAnOwner])),
      oneOfVariant(statusCode(Conflict).and(jsonBodyTypedError[ExamCannotBeClosed]))
    ))
    .in(path[ExamId]("examId"))
    .in("close")
    .post
