package infrastructure.auth

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.all.*
import infrastructure.auth.{AuthenticationError, ForbiddenResource, UnauthorizedAccess, userRoleKey}
import sttp.tapir.Endpoint
import sttp.tapir.server.PartialServerEndpoint
import user.UserRole

class AuthenticationService(tokenSignatureService: TokenSignatureService):
  private def retrieveRole(endpoint: Endpoint[?, ?, ?, ?, ?]): Option[UserRole] =
    endpoint.attribute(userRoleKey)

  extension [I, E >: AuthenticationError, O, R](securedEndpoint: Endpoint[String, I, E, O, R])
    def authenticate: PartialServerEndpoint[String, AuthenticatedUser, I, E, O, R, IO] =
      securedEndpoint.serverSecurityLogic: token =>
        (for
          authenticatedUser <- EitherT(
            tokenSignatureService
              .validate(token)
              .map(_.toRight(UnauthorizedAccess("Invalid or expired token")))
          )
          _ <- EitherT.fromEither[IO](
            if retrieveRole(securedEndpoint).forall(_ == authenticatedUser.role)
            then ().asRight
            else ForbiddenResource("Insufficient permissions").asLeft
          )
        yield authenticatedUser).value
