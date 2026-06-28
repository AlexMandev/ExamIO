package user

import doobie.Meta
import sttp.tapir.Schema
import io.circe.{Codec, Decoder, Encoder}
import io.circe.derivation.ConfiguredEnumCodec
import utils.DerivationConfiguration.given

import java.util.UUID

given io.circe.derivation.Configuration

enum UserRole derives ConfiguredEnumCodec, Schema:
  case STUDENT, TEACHER

object UserRole:
  given Meta[UserRole] = Meta[String].imap(s => UserRole.valueOf(s.toUpperCase))(_.toString.toLowerCase)

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
