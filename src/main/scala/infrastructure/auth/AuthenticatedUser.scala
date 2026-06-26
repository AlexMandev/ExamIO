package infrastructure.auth

import io.circe.Codec
import user.UserRole

import java.util.UUID

case class AuthenticatedUser(id: UUID, role: UserRole) derives Codec
