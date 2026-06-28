package exam

import java.util.UUID
import java.time.Instant

import doobie.Meta

import sttp.tapir.Schema
import io.circe.derivation.ConfiguredEnumCodec

import utils.DerivationConfiguration.given

import io.circe.Codec

enum ExamStatus derives ConfiguredEnumCodec, Schema:
  case DRAFT, OPEN, CLOSED, GRADED

object ExamStatus:
  given Meta[ExamStatus] = Meta[String].imap(s => ExamStatus.valueOf(s.toUpperCase))(_.toString.toLowerCase)

case class Exam(
  id: UUID,
  name: String,
  description: Option[String],
  timeLimitMinutes: Int,
  teacherId: UUID,
  status: ExamStatus,
  createdAt: Instant
) derives Codec,
      Schema

case class NewExam(
  id: UUID,
  name: String,
  description: Option[String],
  timeLimitMinutes: Int,
  teacherId: UUID
)
