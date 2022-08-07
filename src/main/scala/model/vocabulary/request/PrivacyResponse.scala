package io.blindnet.privacy
package model.vocabulary.request

import java.time.Instant

import cats.data.*
import cats.implicits.*
import cats.kernel.Semigroup
import model.vocabulary.*
import model.vocabulary.terms.*
import io.circe.Json

case class PrivacyResponse(
    id: String,
    responseId: String,
    demandId: String,
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
