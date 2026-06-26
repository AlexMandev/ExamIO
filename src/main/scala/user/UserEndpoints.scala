package user

import infrastructure.ExamIOEndpoints.apiBaseEndpoint
import sttp.model.StatusCode.{BadRequest, Conflict, Created}
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody

object UserEndpoints:
  private val baseUsersEndpoint = apiBaseEndpoint.in("users")

  val registerUserEndpoint = baseUsersEndpoint
    .in("register")
    .in(jsonBody[UserRegistrationForm])
    .errorOut(statusCode(BadRequest).and(jsonBody[UserRegistrationError]))
    .out(statusCode(Created).and(jsonBody[User]))
    .post
