package infrastructure.config

case class JwtConfig(secret: String, expirationSeconds: Long)
