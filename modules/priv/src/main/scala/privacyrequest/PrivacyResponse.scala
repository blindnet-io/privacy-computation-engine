package io.blindnet.pce
package priv
package privacyrequest

import java.time.Instant
import java.util.UUID

import io.circe.Json
import terms.*

case class PrivacyResponse(
    id: UUID,
    responseId: UUID,
    demandId: UUID,
    timestamp: Instant,
    action: Action,
    status: Status,
    answer: Option[Json] = None,
    message: Option[String] = None,
    lang: Option[String] = None,
    system: Option[String] = None,
    includes: List[PrivacyResponse] = List.empty,
    data: Option[String] = None
)

object PrivacyResponse {}
