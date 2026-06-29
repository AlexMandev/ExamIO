package user

import cats.effect.{IO, Resource}
import infrastructure.auth.{AuthenticationService, TokenSignatureService}
import infrastructure.db.DBDoobie.DBTransactor
import sttp.tapir.server.ServerEndpoint

case class UserModule(
  userRepository: UserRepository,
  userService: UserService,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object UserModule:
  def apply(
    dbTransactor: DBTransactor,
    tokenSignatureService: TokenSignatureService,
    authenticationService: AuthenticationService
  ): Resource[IO, UserModule] =
    val repository = UserRepository(dbTransactor)
    val service = UserService(repository, tokenSignatureService)
    val controller = UserController(service, authenticationService)

    Resource.pure(UserModule(repository, service, controller.endpoints))
