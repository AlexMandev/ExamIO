package user

import cats.effect.IO
import cats.syntax.all.*
import cats.effect.implicits.*

class UserController(userService: UserService):
  def registerUser =
    UserEndpoints.registerUserEndpoint.serverLogic: userRegistration =>
      userService.registerUser(userRegistration)

  val endpoints = List(registerUser)
