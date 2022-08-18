package io.blindnet.pce
package priv

import terms.*
import io.circe.*
import io.circe.generic.semiauto.*
import util.parsing.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import java.util.UUID

case class LegalBase(
    id: UUID,
    lbType: LegalBaseTerms,
    scope: PrivacyScope,
    name: Option[String] = None,
    description: Option[String] = None,
    active: Boolean
)

object LegalBase {
  given Decoder[LegalBase] = unSnakeCaseIfy(deriveDecoder[LegalBase])
  given Encoder[LegalBase] = snakeCaseIfy(deriveEncoder[LegalBase])

  given Schema[LegalBase] =
    Schema.derived[LegalBase](using Configuration.default.withSnakeCaseMemberNames)

}
