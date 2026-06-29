package exam

import infrastructure.ExamIOEndpoints.apiBaseEndpoint
import sttp.tapir.generic.auto.*
import infrastructure.ExamIOEndpoints.*
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody
import sttp.model.StatusCode.{BadRequest, Created, NotFound}
import cats.data.NonEmptyChain
import sttp.tapir.integ.cats.codec.*
import user.UserRole
import java.util.UUID

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

  // TODO: extend possible status codes with Forbidden and Conflict
  // (tapir types are NOT cooperating it seems)
  val openExamEndpoint = baseExamEndpoint
    .secure(UserRole.TEACHER)
    .in(path[UUID]("examId"))
    .in("open")
    .errorOutVariant(oneOfVariant(statusCode(NotFound).and(jsonBody[ExamError])))
    .post

  val closeExamEndpoint = baseExamEndpoint
    .secure(UserRole.TEACHER)
    .in(path[UUID]("examId"))
    .in("close")
    .errorOutVariant(oneOfVariant(statusCode(NotFound).and(jsonBody[ExamError])))
    .post
