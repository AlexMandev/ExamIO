package exam

import io.circe.Codec
import sttp.tapir.Schema

import cats.data.ValidatedNec
import cats.syntax.all.*
import cats.implicits.*

import utils.ValidationUtils.validateToNec
import utils.DerivationConfiguration.given

type ExamValidation[A] = ValidatedNec[ExamFormError, A]

case class ExamForm(
  name: String,
  description: Option[String],
  timeLimitMinutes: Int
) derives Codec,
      Schema

object ExamForm:
  def validate(form: ExamForm): ExamValidation[ExamForm] =
    (validateName(form.name), validateDescription(form.description), validateTimeLimit(form.timeLimitMinutes))
      .mapN(ExamForm.apply)

  def validateName(name: String): ExamValidation[String] =
    val trimmedName = name.trim

    (validateToNec(trimmedName, InvalidNameError("Name cannot be empty"))(_.nonEmpty) combine
      validateToNec(
        trimmedName,
        InvalidNameError("Name must be <= 100 characters")
      )(_.length <= 100)).map(_ => trimmedName)

  def validateDescription(description: Option[String]): ExamValidation[Option[String]] =
    validateToNec(description, InvalidDescriptionError("Description must not be blank or empty"))(desc =>
      desc.fold(true)(!_.isBlank)
    )

  def validateTimeLimit(timeLimit: Int): ExamValidation[Int] =
    validateToNec(timeLimit, InvalidTimeLimitError("Time limit must be at least 5 minutes"))(_ >= 5)

sealed trait ExamFormError derives Codec, Schema
case class InvalidNameError(msg: String) extends ExamFormError
case class InvalidDescriptionError(msg: String) extends ExamFormError
case class InvalidTimeLimitError(msg: String) extends ExamFormError
