package user

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.*
import io.circe.Codec
import sttp.tapir.Schema

import utils.ValidationUtils
import utils.DerivationConfiguration.given

type ValidationResult[A] = ValidatedNec[RegistrationFormError, A]

case class UserRegistrationForm(
  email: String,
  password: String,
  firstName: String,
  lastName: String,
  role: String
) derives Codec,
      Schema

object UserRegistrationForm:
  private val emailRegex = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$".r
  private val nameRegex = "^[a-zA-Z\\s\\-']+$".r

  def validate(form: UserRegistrationForm): ValidationResult[NewUser] =
    (
      validateEmail(form.email),
      validatePassword(form.password),
      validateName(form.firstName),
      validateName(form.lastName),
      validateUserRole(form.role)
    ).mapN(NewUser.apply)

  private def validateEmail(email: String): ValidationResult[String] =
    ValidationUtils.validateToNec(email.trim, EmailError(email))(e => emailRegex.matches(e))

  private def validatePassword(password: String): ValidationResult[String] =
    ValidationUtils.validateToNec(password, PasswordError(password))(_.length >= 8)

  private def validateName(name: String): ValidationResult[String] =
    ValidationUtils.validateToNec(name.trim, NameError(name))(n => n.nonEmpty && nameRegex.matches(n))

  private def validateUserRole(role: String): ValidationResult[UserRole] =
    val userRoleValues = UserRole.values.map(_.toString)
    ValidationUtils
      .validateToNec(role.trim.toUpperCase, UserRoleError(role))(userRoleValues.contains)
      .map(UserRole.valueOf)

sealed trait RegistrationFormError derives Codec, Schema
case class EmailError(email: String) extends RegistrationFormError
case class PasswordError(password: String) extends RegistrationFormError
case class NameError(name: String) extends RegistrationFormError
case class UserRoleError(userRole: String) extends RegistrationFormError
