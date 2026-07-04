package answer

import cats.data.ValidatedNec
import cats.syntax.all.*
import cats.data.NonEmptyList
import io.circe.Codec
import sttp.tapir.Schema
import sttp.tapir.integ.cats.codec.schemaForNel
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

        case MultipleChoice(choices) =>
          validateOptionIndices(choices, answerValidationCtx.multipleChoiceOptionsCount)
            .map(MultipleChoice.apply)

        case ans => ans.validNec
    )
      .map(AnswerForm.apply)

  def validateOptionIndices(indices: Set[Int], optionsCount: Int): AnswerValidation[Set[Int]] =
    val invalidIndices = indices.filter(i => i < 0 || i >= optionsCount)

    NonEmptyList.fromList(invalidIndices.toList) match
      case None => indices.validNec
      case Some(invalid) => InvalidOptionIndices(invalid).invalidNec

  def validateShortAnswerLimit(answer: String, limit: Int): AnswerValidation[String] =
    if answer.length <= limit then answer.validNec
    else ShortAnswerExceedsLimit(limit).invalidNec

sealed trait AnswerFormError derives Codec, Schema
case class InvalidOptionIndices(indices: NonEmptyList[Int]) extends AnswerFormError derives Codec.AsObject, Schema
case class ShortAnswerExceedsLimit(limit: Int) extends AnswerFormError derives Codec.AsObject, Schema
