package infrastructure

import infrastructure.auth.{AuthenticationError, ForbiddenResource, UnauthorizedAccess, userRoleKey}
import sttp.model.StatusCode.{Forbidden, Unauthorized}
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody
import user.UserRole

import utils.jsonBodyTypedError

object ExamIOEndpoints:
  val apiBaseEndpoint: PublicEndpoint[Unit, Unit, Unit, Any] = endpoint.in("api").in("v1")

  extension [I, O, R](endpoint: PublicEndpoint[I, Unit, O, R])
    def secure: Endpoint[String, I, AuthenticationError, O, R] =
      endpoint.secure(None)

    def secure(role: UserRole): Endpoint[String, I, AuthenticationError, O, R] =
      endpoint.secure(Some(role))

    def secure(maybeRole: Option[UserRole]): Endpoint[String, I, AuthenticationError, O, R] =
      val securedEndpoint = endpoint
        .securityIn(auth.bearer[String]())
        .errorOut(
          oneOf[AuthenticationError](
            oneOfVariant(statusCode(Unauthorized).and(jsonBodyTypedError[UnauthorizedAccess])),
            oneOfVariant(statusCode(Forbidden).and(jsonBodyTypedError[ForbiddenResource]))
          )
        )
      maybeRole
        .map(r => securedEndpoint.attribute(userRoleKey, r))
        .getOrElse(securedEndpoint)
