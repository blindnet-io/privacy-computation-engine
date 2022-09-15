package io.blindnet.pce
package priv

import java.util.UUID

import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import terms.*

case class LegalBase(
    id: UUID,
    lbType: LegalBaseTerms,
    scope: PrivacyScope,
    name: Option[String] = None,
    description: Option[String] = None,
    active: Boolean
) {
  def isConsent            = lbType == LegalBaseTerms.Consent
  def isContract           = lbType == LegalBaseTerms.Contract
  def isLegitimateInterest = lbType == LegalBaseTerms.LegitimateInterest

  def withGranularPS(ctx: PSContext) = this.copy(scope = scope.zoomIn(ctx))
}

object LegalBase {
  given Decoder[LegalBase] = unSnakeCaseIfy(deriveDecoder[LegalBase])
  given Encoder[LegalBase] = snakeCaseIfy(deriveEncoder[LegalBase])

  given Schema[LegalBase] =
    Schema.derived[LegalBase](using Configuration.default.withSnakeCaseMemberNames)

}
