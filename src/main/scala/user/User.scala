package user

import doobie.Meta
import doobie.postgres.implicits.*
import sttp.tapir
import sttp.tapir.{CodecFormat, Schema}
import sttp.tapir.Schema.derivedUnion
import io.circe.{Codec, Decoder, Encoder}
import io.circe.derivation.ConfiguredEnumCodec
import utils.DerivationConfiguration.given

import java.util.UUID

given io.circe.derivation.Configuration

enum UserRole derives ConfiguredEnumCodec, Schema:
  case STUDENT, TEACHER

object UserRole:
  given Meta[UserRole] = Meta[String].imap(s => UserRole.valueOf(s.toUpperCase))(_.toString.toLowerCase)

opaque type TeacherId = UUID

object TeacherId:
  def apply(id: UUID): TeacherId = id
  extension (teacherId: TeacherId) def value: UUID = teacherId

  given Codec[TeacherId] = Codec.implied[UUID]
  given Schema[TeacherId] = Schema.string[UUID].format("uuid")
  given Meta[TeacherId] = Meta[UUID].imap(TeacherId.apply)(_.value)

  given tapir.Codec[String, TeacherId, CodecFormat.TextPlain] =
    tapir.Codec.uuid.map(TeacherId.apply)(_.value)

opaque type StudentId = UUID

object StudentId:
  def apply(id: UUID): StudentId = id
  extension (studentId: StudentId) def value: UUID = studentId

  given Codec[StudentId] = Codec.implied[UUID]
  given Schema[StudentId] = Schema.string[UUID].format("uuid")
  given Meta[StudentId] = Meta[UUID].imap(StudentId.apply)(_.value)

  given tapir.Codec[String, StudentId, CodecFormat.TextPlain] =
    tapir.Codec.uuid.map(StudentId.apply)(_.value)


case class User(
  id: UUID,
  email: String,
  passwordHash: String,
  firstName: String,
  lastName: String,
  role: UserRole
) derives Codec,
      Schema

case class NewUser(
  email: String,
  password: String,
  firstName: String,
  lastName: String,
  role: UserRole
)
