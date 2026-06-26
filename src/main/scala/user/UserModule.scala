package user

import cats.effect.IO
import cats.effect.kernel.Resource
import infrastructure.db.DBDoobie.DBTransactor
import sttp.tapir.server.ServerEndpoint

case class UserModule(
  userRepository: UserRepository,
  userService: UserService,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object UserModule:
  def apply(dbTransactor: DBTransactor): Resource[IO, UserModule] =
    val repository = UserRepository(dbTransactor)
    val service = UserService(repository)
    val controller = UserController(service)

    Resource.pure(UserModule(repository, service, controller.endpoints))
