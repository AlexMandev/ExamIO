package user

import infrastructure.ExamIOEndpoints.*
import sttp.model.StatusCode.{BadRequest, Created, Unauthorized}
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

  val loginEndpoint = baseUsersEndpoint
    .in("login")
    .in(jsonBody[UserLoginForm])
    .out(jsonBody[String])
    .errorOut(statusCode(Unauthorized))
    .post
