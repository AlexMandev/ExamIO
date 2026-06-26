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

  // test endpoints - delete after testing
  val anyUserEndpoint = baseUsersEndpoint
    .in("me")
    .out(jsonBody[String])
    .get
    .secure

  val teacherOnlyEndpoint = baseUsersEndpoint
    .in("teacher-only")
    .out(jsonBody[String])
    .get
    .secure(UserRole.TEACHER)

  val studentOnlyEndpoint = baseUsersEndpoint
    .in("student-only")
    .out(jsonBody[String])
    .get
    .secure(UserRole.STUDENT)
