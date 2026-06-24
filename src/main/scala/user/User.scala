package user

import doobie.Meta
import java.util.UUID

enum UserRole:
  case STUDENT, TEACHER

object UserRole:
  given Meta[UserRole] = Meta[String].imap(s => UserRole.valueOf(s.toUpperCase))(_.toString)

case class User(
  id: UUID,
  email: String,
  passwordHash: String,
  firstName: String,
  lastName: String,
  role: UserRole
)
