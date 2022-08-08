package io.blindnet.privacy
package model.vocabulary.request

import java.time.Instant
import java.util.UUID

import cats.data.*
import cats.implicits.*
import cats.kernel.Semigroup
import io.circe.Json
import model.vocabulary.*
import model.vocabulary.terms.*

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
