package client

import cats.effect.IO
import cats.implicits.*
import cats.syntax.all.*
import user.{
  EmailError,
  NameError,
  PasswordError,
  RegistrationFormError,
  RegistrationValidationError,
  UserAlreadyExistsError,
  UserLoginForm,
  UserRegistrationError,
  UserRegistrationForm,
  UserRoleError,
  UserRole
}

def loginFlow(client: ExamIOApiClient, role: UserRole): IO[Option[String]] =
  for
    email <- promptForString("Email: ")
    password <- promptForString("Password: ")
    result <- client.login(UserLoginForm(email, password))
    token <- result.fold(
      _ => IO.println("Invalid credentials.").as(None),
      token => IO.println("Login successful!").as(Some(token))
    )
  yield token

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
