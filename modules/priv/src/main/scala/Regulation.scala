package io.blindnet.pce
package priv

import terms.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import java.util.UUID

case class Regulation(
    id: UUID,
    prohibitedScope: Map[LegalBaseTerms, PrivacyScope]
)

object Regulation {}

// TODO: probably merge
case class RegulationInfo(
    id: UUID,
    name: String,
    description: Option[String]
)

object RegulationInfo {}
