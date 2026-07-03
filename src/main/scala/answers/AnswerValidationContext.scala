package answer

import io.circe.Codec
import sttp.tapir.Schema

case class AnswerValidationContext(
  shortAnswerLimit: Int,
  multipleChoiceOptionsCount: Int
) derives Codec, Schema
