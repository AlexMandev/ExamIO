package user

import infrastructure.auth.AuthenticationService

class UserController(userService: UserService, authService: AuthenticationService):
  def registerUser =
    UserEndpoints.registerUserEndpoint.serverLogic: form =>
      userService.registerUser(form)

  def login =
    UserEndpoints.loginEndpoint.serverLogic: form =>
      userService.login(form).map(_.toRight(()))

  val endpoints = List(registerUser, login)
