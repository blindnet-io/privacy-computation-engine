package io.blindnet.privacy
package model.vocabulary.terms

import cats.data.Validated
import io.circe.*
import doobie.util.Get

enum EventTerms(term: String) {
  case CaptureDate       extends EventTerms("CAPTURE-DATE")
  case RelationshipStart extends EventTerms("RELATIONSHIP-START")
  case RelationshipEnd   extends EventTerms("RELATIONSHIP-END")
  case ServiceStart      extends EventTerms("SERVICE-START")
  case ServiceEnd        extends EventTerms("SERVICE-END")

  def isTerm(str: String) = term == str

  val encode = term
}

object EventTerms {
  def parse(str: String): Validated[String, EventTerms] =
    Validated.fromOption(
      EventTerms.values.find(a => a.isTerm(str)),
      "Unknown event term"
    )

  def parseUnsafe(str: String): EventTerms =
    EventTerms.values.find(a => a.isTerm(str)).get

  given Decoder[EventTerms] =
    Decoder.decodeString.emap(EventTerms.parse(_).toEither)

  given Encoder[EventTerms] =
    Encoder[String].contramap(_.encode)

  given Get[EventTerms] =
    Get[String].map(t => EventTerms.parseUnsafe(t))

}
