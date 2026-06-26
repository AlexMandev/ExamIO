package user

import cats.effect.IO
import infrastructure.auth.AuthenticationService

class UserController(userService: UserService, authService: AuthenticationService):
  import authService.*

  def registerUser =
    UserEndpoints.registerUserEndpoint.serverLogic: form =>
      userService.registerUser(form)

  def login =
    UserEndpoints.loginEndpoint.serverLogic: form =>
      userService.login(form).map(_.toRight(()))

  val endpoints = List(registerUser, login)
