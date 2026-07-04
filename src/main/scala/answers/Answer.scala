package answers

import io.circe.{Codec, Json}
import io.circe.derivation.ConfiguredEnumCodec
import io.circe.syntax.*

import utils.DerivationConfiguration.given
import utils.DoobieUtils.given

import sttp.tapir
import sttp.tapir.Schema

import doobie.Meta
import doobie.postgres.implicits.*

import question.{QuestionId, Question, QuestionType}
import submission.SubmissionId

enum AnswerType derives ConfiguredEnumCodec, Schema:
  case MultipleChoice, TrueFalse, ShortAnswer

object AnswerType:
  given Meta[AnswerType] = Meta[String].imap(AnswerType.valueOf)(_.toString)

sealed trait AnswerData derives Codec, Schema
case class MultipleChoice(answerIndex: Int) extends AnswerData
case class TrueFalse(answer: Option[Boolean]) extends AnswerData
case class ShortAnswer(answer: String) extends AnswerData

object AnswerData:
  given Meta[AnswerData] =
    Meta[Json].tiemap(_.as[AnswerData].toOption.toRight("Invalid db parsing of question data"))(_.asJson)

extension (answerData: AnswerData)
  def toType: AnswerType =
    answerData match
      case _: TrueFalse => AnswerType.TrueFalse
      case _: MultipleChoice => AnswerType.MultipleChoice
      case _: ShortAnswer => AnswerType.ShortAnswer

case class Answer(
  questionId: QuestionId,
  submissionId: SubmissionId,
  data: AnswerData,
  pointsAwarded: Option[BigDecimal] = None
) derives Codec,
      Schema

extension (answer: Answer)
  def matchType(question: Question): Boolean =
    answer.data match
      case _: TrueFalse => question.questionType == QuestionType.TrueFalse
      case _: MultipleChoice => question.questionType == QuestionType.MultipleChoice
      case _: ShortAnswer => question.questionType == QuestionType.ShortAnswer
