package answer

import cats.data.ValidatedNec
import cats.syntax.all.*
import io.circe.Codec
import sttp.tapir.Schema
import utils.ValidationUtils.validateToNec
import utils.DerivationConfiguration.given

type AnswerValidation[A] = ValidatedNec[AnswerFormError, A]

case class AnswerForm(
  answerData: AnswerData
) derives Codec,
      Schema

object AnswerForm:
  def validate(answerForm: AnswerForm, answerValidationCtx: AnswerValidationContext): AnswerValidation[AnswerForm] =
    (
      answerForm.answerData match
        case ShortAnswer(answer) =>
          validateShortAnswerLimit(answer, answerValidationCtx.shortAnswerLimit)
            .map(ShortAnswer.apply)

        case MultipleChoice(index) =>
          validateOptionIndex(index, answerValidationCtx.multipleChoiceOptionsCount)
            .map(MultipleChoice.apply)

        case ans => ans.validNec
    )
      .map(AnswerForm.apply)

  def validateOptionIndex(index: Int, optionsCount: Int): AnswerValidation[Int] =
    if index >= 0 && index < optionsCount then index.validNec
    else InvalidOptionIndex(index).invalidNec

  def validateShortAnswerLimit(answer: String, limit: Int): AnswerValidation[String] =
    if answer.length <= limit then answer.validNec
    else ShortAnswerExceedsLimit(limit).invalidNec

sealed trait AnswerFormError derives Codec, Schema
case class InvalidOptionIndex(index: Int) extends AnswerFormError derives Codec.AsObject, Schema
case class ShortAnswerExceedsLimit(limit: Int) extends AnswerFormError derives Codec.AsObject, Schema
