package io.blindnet.privacy
package model.vocabulary.terms

import cats.data.Validated
import io.circe.*

enum EventTerms(term: String) {
  case CaptureDate       extends EventTerms("CAPTURE-DATE")
  case RelationshipStart extends EventTerms("RELATIONSHIP-END")
  case RelationshipEnd   extends EventTerms("RELATIONSHIP-START")
  case ServiceStart      extends EventTerms("SERVICE-END")
  case ServiceEnd        extends EventTerms("SERVICE-START")

  def isTerm(str: String) = term == str

  val encode = term
}

object EventTerms {
  def parse(str: String): Validated[String, EventTerms] =
    Validated.fromOption(
      EventTerms.values.find(a => a.isTerm(str)),
      "Unknown event term"
    )

  given Decoder[EventTerms] =
    Decoder.decodeString.emap(EventTerms.parse(_).toEither)

  given Encoder[EventTerms] =
    Encoder[String].contramap(_.encode)

}
