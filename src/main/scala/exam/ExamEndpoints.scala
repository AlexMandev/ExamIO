package exam

import infrastructure.ExamIOEndpoints.apiBaseEndpoint
import sttp.tapir.generic.auto.*
import infrastructure.ExamIOEndpoints.*
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody
import sttp.model.StatusCode.{BadRequest, Created}
import cats.data.NonEmptyChain
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
