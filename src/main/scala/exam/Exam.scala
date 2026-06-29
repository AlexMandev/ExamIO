package exam

import java.util.UUID
import java.time.Instant

import doobie.Meta
import doobie.postgres.implicits.*

import io.circe.Codec
import io.circe.derivation.ConfiguredEnumCodec

import sttp.tapir
import sttp.tapir.Schema
import sttp.tapir.SchemaType
import sttp.tapir.{CodecFormat, Schema, SchemaType}

import utils.DerivationConfiguration.given

enum ExamStatus derives ConfiguredEnumCodec, Schema:
  case DRAFT, OPEN, CLOSED, GRADED

object ExamStatus:
  given Meta[ExamStatus] = Meta[String].imap(s => ExamStatus.valueOf(s.toUpperCase))(_.toString.toLowerCase)

opaque type ExamId = UUID

object ExamId:
  def apply(id: UUID): ExamId = id
  extension (examId: ExamId) def value: UUID = examId

  given Codec[ExamId] = Codec.implied[UUID]
  given Schema[ExamId] = Schema.string[UUID].format("uuid")
  given Meta[ExamId] = Meta[UUID].imap(ExamId.apply)(_.value)

  given tapir.Codec[String, ExamId, CodecFormat.TextPlain] =
    tapir.Codec.uuid.map(ExamId.apply)(_.value)

opaque type TeacherId = UUID

object TeacherId:
  def apply(id: UUID): TeacherId = id
  extension (teacherId: TeacherId) def value: UUID = teacherId

  given Codec[TeacherId] = Codec.implied[UUID]
  given Schema[TeacherId] = Schema.string[UUID].format("uuid")
  given Meta[TeacherId] = Meta[UUID].imap(TeacherId.apply)(_.value)

  given tapir.Codec[String, TeacherId, CodecFormat.TextPlain] =
    tapir.Codec.uuid.map(TeacherId.apply)(_.value)

case class Exam(
  id: ExamId,
  name: String,
  description: Option[String],
  timeLimitMinutes: Int,
  teacherId: TeacherId,
  status: ExamStatus,
  createdAt: Instant
) derives Codec,
      Schema

case class NewExam(
  id: ExamId,
  name: String,
  description: Option[String],
  timeLimitMinutes: Int,
  teacherId: TeacherId
)
