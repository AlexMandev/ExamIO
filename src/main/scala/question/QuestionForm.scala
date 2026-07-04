package question

import cats.data.ValidatedNec
import cats.syntax.all.*
import io.circe.Codec
import sttp.tapir.Schema
import utils.ValidationUtils.validateToNec
import utils.DerivationConfiguration.given

type QuestionValidation[A] = ValidatedNec[QuestionFormError, A]

case class QuestionForm(
  questionText: String,
  points: BigDecimal,
  data: QuestionData
) derives Codec,
      Schema

object QuestionForm:
  def validate(form: QuestionForm): QuestionValidation[QuestionForm] =
    (validateText(form.questionText), validatePoints(form.points), validateData(form.data))
      .mapN(QuestionForm.apply)

  private def validateText(text: String): QuestionValidation[String] =
    val trimmed = text.trim
    (
      validateToNec(trimmed, InvalidQuestionText("Question text cannot be empty"))(_.nonEmpty),
      validateToNec(trimmed, InvalidQuestionText("Question text must be <= 1000 characters"))(_.length <= 1000)
    ).mapN((_, _) => trimmed)

  private def validatePoints(points: BigDecimal): QuestionValidation[BigDecimal] =
    validateToNec(points, InvalidPoints("Points must be greater than 0"))(_ > 0)

  private def validateData(data: QuestionData): QuestionValidation[QuestionData] =
    data match
      case MultipleChoiceData(options, idx) =>
        (
          validateToNec(options, InvalidQuestionData("Multiple choice requires at least 2 options"))(_.length >= 2),
          validateToNec(idx, InvalidQuestionData("Correct option index is out of range"))(i =>
            i >= 0 && i < options.length
          )
        ).mapN((_, _) => data)
      case TrueFalseData(_) => data.validNec
      case ShortAnswerData(_) => data.validNec

sealed trait QuestionFormError derives Codec, Schema
case class InvalidQuestionText(msg: String) extends QuestionFormError
case class InvalidPoints(msg: String) extends QuestionFormError
case class InvalidQuestionData(msg: String) extends QuestionFormError
