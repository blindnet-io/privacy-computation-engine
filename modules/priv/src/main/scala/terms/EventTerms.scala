package io.blindnet.pce
package priv
package terms

import cats.data.Validated
import doobie.util.Get
import io.circe.*
import sttp.tapir.{Schema, Validator}

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

  given Schema[EventTerms] =
    Schema.string.validate(
      Validator.enumeration(EventTerms.values.toList, x => Option(x.encode))
    )

  given Get[EventTerms] =
    Get[String].map(t => EventTerms.parseUnsafe(t))

}
