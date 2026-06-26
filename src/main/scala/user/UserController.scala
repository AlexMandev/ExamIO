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

  // test endpoints - delete after testing
  def me =
    UserEndpoints.anyUserEndpoint.authenticate.serverLogic: user =>
      _ => IO.pure(Right(s"Hello, ${user.id} with role ${user.role}"))

  def teacherOnly =
    UserEndpoints.teacherOnlyEndpoint.authenticate.serverLogic: user =>
      _ => IO.pure(Right(s"Hello teacher ${user.id}"))

  def studentOnly =
    UserEndpoints.studentOnlyEndpoint.authenticate.serverLogic: user =>
      _ => IO.pure(Right(s"Hello student ${user.id}"))

  val endpoints = List(registerUser, login, me, teacherOnly, studentOnly)
