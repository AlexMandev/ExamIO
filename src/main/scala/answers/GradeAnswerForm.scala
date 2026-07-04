package answer

import io.circe.Codec
import sttp.tapir.Schema
import utils.DerivationConfiguration.given

case class GradeAnswerForm(points: BigDecimal) derives Codec, Schema

case class InvalidPointsAwarded(message: String) derives Codec.AsObject, Schema
