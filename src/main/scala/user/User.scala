package user

import doobie.Meta
import sttp.tapir.Schema
import io.circe.{Codec, Decoder, Encoder}

import java.util.UUID

enum UserRole derives Codec, Schema:
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
