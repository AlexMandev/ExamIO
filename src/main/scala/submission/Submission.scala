package submission

import io.circe.Codec
import io.circe.derivation.ConfiguredEnumCodec
import utils.DerivationConfiguration.given

import sttp.tapir
import sttp.tapir.{CodecFormat, Schema}

import doobie.Meta
import doobie.postgres.implicits.*

import java.util.UUID
import java.time.Instant

import exam.ExamId
import user.StudentId

opaque type SubmissionId = UUID

object SubmissionId:
  def apply(id: UUID): SubmissionId = id
  extension (submissionId: SubmissionId) def value: UUID = submissionId

  given Codec[SubmissionId] = Codec.implied[UUID]
  given Schema[SubmissionId] = Schema.string[UUID].format("uuid")
  given Meta[SubmissionId] = Meta[UUID].imap(SubmissionId.apply)(_.value)

  given tapir.Codec[String, SubmissionId, CodecFormat.TextPlain] =
    tapir.Codec.uuid.map(SubmissionId.apply)(_.value)

enum SubmissionStatus derives ConfiguredEnumCodec, Schema:
  case InProgress, Finished, Graded

object SubmissionStatus:
  given Meta[SubmissionStatus] = Meta[String].imap(SubmissionStatus.valueOf)(_.toString)

case class Submission(
  id: SubmissionId,
  examId: ExamId,
  studentId: StudentId,
  startedAt: Instant,
  finishedAt: Option[Instant],
  status: SubmissionStatus,
) derives Codec, Schema

case class NewSubmission(
  id: SubmissionId,
  examId: ExamId,
  studentId: StudentId
) derives Codec, Schema
