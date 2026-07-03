package answer

import io.circe.Codec
import io.circe.derivation.ConfiguredEnumCodec
import utils.DerivationConfiguration.given

import sttp.tapir
import sttp.tapir.{CodecFormat, Schema}

import doobie.Meta
import doobie.postgres.implicits.*

import java.util.UUID

import question.QuestionId
import submission.SubmissionId

enum AnswerType derives Codec, Schema:
  case MultipleChoice, TrueFalse, ShortAnswer

object AnswerType:
  given Meta[AnswerType] = Meta[String].imap(AnswerType.valueOf)(_.toString)

sealed trait AnswerData derives Codec, Schema
case class MultipleChoice(answerIndices: Set[Int]) extends AnswerData
case class TrueFalse(answer: Option[Boolean]) extends AnswerData
case class ShortAnswer(answer: String) extends AnswerData

case class Answer(
  questionId: QuestionId,
  submissionId: SubmissionId,
  tipe: AnswerType,
  data: AnswerData
) derives Codec, Schema
