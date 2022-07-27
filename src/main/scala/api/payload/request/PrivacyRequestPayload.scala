package io.blindnet.privacy
package api.endpoints.payload.request

import cats.effect.*
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import model.vocabulary.*
import model.vocabulary.terms.*

case class Restriction()

case class PrivacyRequestDemand(
    id: String,
    action: Action,
    message: Option[String],
    language: Option[String],
    data: Option[List[String]],
    restrictions: Option[List[Restriction]],
    target: Option[Target]
)

case class PrivacyRequestPayload(
    demands: List[PrivacyRequestDemand],
    dataSubject: List[DataSubject]
)

given Decoder[PrivacyRequestPayload] =
  Decoder.forProduct2("demands", "data_subject")(PrivacyRequestPayload.apply)

given EntityDecoder[IO, PrivacyRequestPayload] = jsonOf[IO, PrivacyRequestPayload]
