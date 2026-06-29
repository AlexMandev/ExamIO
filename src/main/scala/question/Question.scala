package question

import java.util.UUID

import doobie.Meta
import doobie.postgres.implicits.*

import io.circe.Codec
import io.circe.derivation.ConfiguredEnumCodec

import sttp.tapir
import sttp.tapir.{CodecFormat, Schema}

import exam.ExamId
import utils.DerivationConfiguration.given

opaque type QuestionId = UUID

object QuestionId:
  def apply(id: UUID): QuestionId = id
  extension (q: QuestionId) def value: UUID = q

  given Codec[QuestionId] = Codec.implied[UUID]
  given Schema[QuestionId] = Schema.string[UUID].format("uuid")
  given Meta[QuestionId] = Meta[UUID].imap(QuestionId.apply)(_.value)

  given tapir.Codec[String, QuestionId, CodecFormat.TextPlain] =
    tapir.Codec.uuid.map(QuestionId.apply)(_.value)

enum QuestionType derives ConfiguredEnumCodec, Schema:
  case MultipleChoice, TrueFalse, ShortAnswer

object QuestionType:
  given Meta[QuestionType] = Meta[String].imap(QuestionType.valueOf)(_.toString)

sealed trait QuestionData derives Codec, Schema

case class MultipleChoiceData(
  options: List[String],
  correctOptionIndex: Int
) extends QuestionData

case class TrueFalseData(
  correctAnswer: Boolean
) extends QuestionData

case class ShortAnswerData() extends QuestionData

case class Question(
  id: QuestionId,
  examId: ExamId,
  questionText: String,
  questionType: QuestionType,
  points: BigDecimal,
  position: Int,
  data: QuestionData
) derives Codec,
      Schema
