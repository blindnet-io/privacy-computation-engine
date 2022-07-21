package io.blindnet.privacy
package api.endpoints.payload

import cats.effect.*
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import model.vocabulary.terms.*

case class PrivacyRequestResponsePayload(
    requestId: String,
    responses: List[Json]
)
