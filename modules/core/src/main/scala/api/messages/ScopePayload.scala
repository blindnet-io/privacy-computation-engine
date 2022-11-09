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

case class ScopePayload(
    dc: DataCategory,
    pc: ProcessingCategory,
    pp: Purpose
)

object ScopePayload {
  given Decoder[ScopePayload] = deriveDecoder[ScopePayload]
  given Encoder[ScopePayload] = deriveEncoder[ScopePayload]
  given Schema[ScopePayload]  = Schema.derived[ScopePayload]
}
