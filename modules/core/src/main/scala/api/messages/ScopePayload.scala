package io.blindnet.pce
package api.endpoints.messages

import cats.effect.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.Schema.annotations.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import priv.terms.*
import io.blindnet.pce.priv.*

case class ScopePayload(
    dataCategories: Set[DataCategory],
    processingCategories: Set[ProcessingCategory],
    processingPurposes: Set[Purpose]
)

object ScopePayload {
  given Decoder[ScopePayload] = unSnakeCaseIfy(deriveDecoder[ScopePayload])
  given Encoder[ScopePayload] = snakeCaseIfy(deriveEncoder[ScopePayload])
  given Schema[ScopePayload]  =
    Schema.derived[ScopePayload](using Configuration.default.withSnakeCaseMemberNames)

  def toPrivacyScope(payload: Set[ScopePayload]) =
    val triples = payload.flatMap(
      s =>
        for
          dc <- s.dataCategories
          pc <- s.processingCategories
          pp <- s.processingPurposes
        yield PrivacyScopeTriple(dc, pc, pp)
    )
    PrivacyScope(triples)

}
