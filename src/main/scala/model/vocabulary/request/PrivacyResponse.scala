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
    responseId: String,
    demandId: String,
    date: Instant,
    requestedAction: Action,
    status: Status,
    answer: Json,
    message: Option[String],
    lang: String,
    includes: Option[String] = None,
    data: Option[String] = None
)

object PrivacyResponse {}
