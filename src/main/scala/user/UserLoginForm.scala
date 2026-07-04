package user

import io.circe.Codec
import sttp.tapir.Schema

case class UserLoginForm(email: String, password: String) derives Codec, Schema

case class LoginResponse(token: String, role: UserRole) derives Codec, Schema
