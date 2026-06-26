package infrastructure.auth

import cats.effect.IO
import infrastructure.config.JwtConfig
import io.circe.syntax.*
import io.circe.parser.decode
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import user.User

import java.time.Instant

class TokenSignatureService(jwtConfig: JwtConfig):
  private val algorithm = JwtAlgorithm.HS256

  def sign(user: User): IO[String] = IO:
    val claim = JwtClaim(
      content = AuthenticatedUser(user.id, user.role).asJson.noSpaces,
      expiration = Some(Instant.now.getEpochSecond + jwtConfig.expirationSeconds)
    )
    JwtCirce.encode(claim, jwtConfig.secret, algorithm)

  def validate(token: String): IO[Option[AuthenticatedUser]] = IO:
    JwtCirce
      .decode(token, jwtConfig.secret, Seq(algorithm))
      .toOption
      .flatMap(claim => decode[AuthenticatedUser](claim.content).toOption)
