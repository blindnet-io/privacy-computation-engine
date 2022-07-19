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

case class Restriction()

case class Demand(
    id: String,
    action: Action,
    message: Option[String],
    language: Option[String],
    data: List[String],
    restrictions: List[Restriction],
    target: Option[Target]
)

case class DataSubject(
    id: String,
    schema: String
)

case class PrivacyRequestPayload(
    demands: List[Demand],
    dataSubject: List[DataSubject]
)

given Decoder[PrivacyRequestPayload] =
  Decoder.forProduct2("demands", "data_subject")(PrivacyRequestPayload.apply)

given EntityDecoder[IO, PrivacyRequestPayload] = jsonOf[IO, PrivacyRequestPayload]
