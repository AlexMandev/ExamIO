package utils

import io.circe.derivation.Configuration
import sttp.tapir.generic.Configuration as TapirConfiguration

object DerivationConfiguration:
  given Configuration = Configuration.default.withDiscriminator("type")

  given TapirConfiguration = TapirConfiguration.default.withDiscriminator("type")
