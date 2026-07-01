package infrastructure.auth

import io.circe.Codec
import sttp.tapir.Schema
import user.UserRole

val userRoleKey = sttp.tapir.AttributeKey[UserRole]

sealed trait AuthenticationError derives Codec, Schema
case class UnauthorizedAccess(message: String) extends AuthenticationError derives Codec.AsObject, Schema
case class ForbiddenResource(message: String) extends AuthenticationError derives Codec.AsObject, Schema
