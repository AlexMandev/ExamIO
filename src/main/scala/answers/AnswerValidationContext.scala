package answer

import io.circe.Codec
import sttp.tapir.Schema

import question.{Question, QuestionData, TrueFalseData, MultipleChoiceData, ShortAnswerData}

case class AnswerValidationContext(
  multipleChoiceOptionsCount: Int,
  shortAnswerLimit: Int
) derives Codec, Schema

object AnswerValidationContext:
  def fromQuestion(question: Question): AnswerValidationContext =
    question.data match
      case TrueFalseData(_) => AnswerValidationContext(Int.MaxValue, Int.MaxValue)
      case MultipleChoiceData(options, _) => AnswerValidationContext(options.length, Int.MaxValue)
      case ShortAnswerData(limit) => AnswerValidationContext(Int.MaxValue, limit)
