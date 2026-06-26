package user

import cats.data.NonEmptyChain
import cats.effect.IO
import cats.syntax.all.*
import io.circe.Codec
import sttp.tapir.Schema
import utils.HashUtils
import sttp.tapir.integ.cats.codec.*

import java.util.UUID

class UserService(userRepository: UserRepository):
  def registerUser(form: UserRegistrationForm): IO[Either[UserRegistrationError, User]] =
    UserRegistrationForm
      .validate(form)
      .fold(
        errors => Left(RegistrationValidationError(errors)).pure[IO],
        newUser => createUser(newUser)
      )

  private def createUser(newUser: NewUser) =
    for
      id = UUID.randomUUID()
      hashedPwd <- IO.blocking(HashUtils.hashPassword(newUser.password))
      registeredUser <- userRepository.registerUser(
        User(id, newUser.email, hashedPwd, newUser.firstName, newUser.lastName, newUser.role)
      )
    yield registeredUser

sealed trait UserRegistrationError derives Codec, Schema
case class RegistrationValidationError(errs: NonEmptyChain[RegistrationFormError]) extends UserRegistrationError
case class UserAlreadyExistsError(email: String) extends UserRegistrationError
