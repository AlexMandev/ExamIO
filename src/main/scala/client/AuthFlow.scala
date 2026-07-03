package client

import CommonFlow.{promptForStringLine, promptForString}

import cats.effect.IO
import cats.implicits.*
import cats.syntax.all.*
import user.{
  EmailError,
  LoginResponse,
  NameError,
  PasswordError,
  RegistrationFormError,
  RegistrationValidationError,
  UserAlreadyExistsError,
  UserLoginForm,
  UserRegistrationError,
  UserRegistrationForm,
  UserRole,
  UserRoleError
}

object AuthFlow:
  def loginFlow(client: ExamIOApiClient): IO[Option[LoginResponse]] =
    for
      email <- promptForString("Email: ")
      password <- promptForString("Password: ")
      result <- client.login(UserLoginForm(email, password))
      response <- result.fold(
        _ => IO.println("Invalid email or password.").as(None),
        resp => IO.pure(Some(resp))
      )
    yield response

  def registerFlow(client: ExamIOApiClient): IO[Unit] =
    for
      email <- promptForString("Email: ")
      password <- promptForString("Password: ")
      firstName <- promptForString("First name: ")
      lastName <- promptForString("Last name: ")
      role <- promptForString("Role (TEACHER/STUDENT): ")
      result <- client.register(UserRegistrationForm(email, password, firstName, lastName, role))
      _ <- result.fold(
        printRegistrationError,
        user => IO.println(s"Registered! Welcome, ${user.firstName}. You can log in now.")
      )
    yield ()

  private def printRegistrationError(err: UserRegistrationError): IO[Unit] =
    err match
      case RegistrationValidationError(errs) =>
        IO.println("Registration failed:") >>
          errs.toList.traverse(e => IO.println(s"  - ${formatFormError(e)}")).as(())
      case UserAlreadyExistsError(email) =>
        IO.println(s"An account with email $email already exists.")

  private def formatFormError(err: RegistrationFormError): String =
    err match
      case EmailError(email) => s"Invalid email address: $email"
      case PasswordError(_) => "Password must be at least 8 characters"
      case NameError(name) => s"Invalid name: $name"
      case UserRoleError(role) => s"Invalid role '$role' — must be TEACHER or STUDENT"
