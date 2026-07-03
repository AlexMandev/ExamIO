package user

import cats.data.NonEmptyChain
import cats.effect.IO
import cats.syntax.all.*
import io.circe.Codec
import sttp.tapir.Schema
import infrastructure.auth.TokenSignatureService
import sttp.tapir.integ.cats.codec.*

import java.util.UUID

import utils.HashUtils.{checkPassword, hashPassword}
import utils.DerivationConfiguration.given

class UserService(userRepository: UserRepository, tokenService: TokenSignatureService):
  def registerUser(form: UserRegistrationForm): IO[Either[UserRegistrationError, User]] =
    UserRegistrationForm
      .validate(form)
      .fold(
        errors => Left(RegistrationValidationError(errors)).pure[IO],
        newUser => createUser(newUser)
      )

  def login(form: UserLoginForm): IO[Option[LoginResponse]] =
    for
      maybeUser <- userRepository.getByEmail(form.email)
      maybeLogin <- maybeUser
        .filter(user => checkPassword(form.password, user.passwordHash))
        .traverse(user => tokenService.sign(user).map(LoginResponse(_, user.role)))
    yield maybeLogin

  private def createUser(newUser: NewUser) =
    for
      id <- IO(UUID.randomUUID())
      hashedPwd <- IO.blocking(hashPassword(newUser.password))
      registeredUser <- userRepository.registerUser(
        User(id, newUser.email, hashedPwd, newUser.firstName, newUser.lastName, newUser.role)
      )
    yield registeredUser

sealed trait UserRegistrationError derives Codec, Schema
case class RegistrationValidationError(errs: NonEmptyChain[RegistrationFormError]) extends UserRegistrationError
case class UserAlreadyExistsError(email: String) extends UserRegistrationError
