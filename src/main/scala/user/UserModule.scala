package user

import cats.effect.{IO, Resource}
import infrastructure.auth.{AuthenticationService, TokenSignatureService}
import infrastructure.db.DBDoobie.DBTransactor
import sttp.tapir.server.ServerEndpoint

case class UserModule(
  userRepository: UserRepository,
  userService: UserService,
  authenticationService: AuthenticationService,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object UserModule:
  def apply(dbTransactor: DBTransactor, tokenSignatureService: TokenSignatureService): Resource[IO, UserModule] =
    val repository = UserRepository(dbTransactor)
    val service = UserService(repository, tokenSignatureService)
    val authService = AuthenticationService(tokenSignatureService)
    val controller = UserController(service, authService)

    Resource.pure(UserModule(repository, service, authService, controller.endpoints))
