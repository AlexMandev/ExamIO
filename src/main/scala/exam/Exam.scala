package exam

import java.util.UUID
import java.time.Instant

import sttp.tapir.Schema
import io.circe.derivation.ConfiguredEnumCodec
import utils.DerivationConfiguration.given

enum ExamStatus derives ConfiguredEnumCodec, Schema:
  case DRAFT, OPEN, CLOSED, GRADED

case class Exam(
  id: UUID,
  name: String,
  description: String,
  timeLimitMinutes: Int,
  teacherId: UUID,
  status: ExamStatus,
  createdAt: Instant
)
