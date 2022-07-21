package io.blindnet.privacy
package model.vocabulary.terms

import cats.data.Validated
import io.circe.*

case class Purpose(term: String)

object Purpose {

  def parse(s: String): Validated[String, Purpose] =
    Validated.fromOption(
      s.split('.').headOption.filter(terms.contains).map(_ => Purpose(s)),
      "Unknown purpose of processing"
    )

  val terms = List(
    "*",
    "ADVERTISING",
    "COMPLIANCE",
    "EMPLOYMENT",
    "JUSTICE",
    "MARKETING",
    "MEDICAL",
    "PERSONALIZATION",
    "PUBLIC-INTERESTS",
    "RESEARCH",
    "SALE",
    "SECURITY",
    "SERVICES",
    "SERVICES.ADDITIONAL-SERVICES",
    "SERVICES.BASIC-SERVICE",
    "SOCIAL-PROTECTION",
    "TRACKING",
    "VITAL-INTERESTS",
    "OTHER-PURPOSE"
  )

  given Decoder[Purpose] =
    Decoder.decodeString.emap(Purpose.parse(_).toEither)

  given Encoder[Purpose] =
    Encoder[String].contramap(_.term)

}
