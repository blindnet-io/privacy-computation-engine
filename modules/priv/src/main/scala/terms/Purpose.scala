package io.blindnet.pce
package priv
package terms

import cats.data.Validated
import doobie.util.Get
import io.circe.*
import sttp.tapir.{ Schema, Validator }

case class Purpose(term: String)

object Purpose {

  def parse(s: String): Validated[String, Purpose] =
    Validated.fromOption(
      terms.find(_ == s).map(_ => Purpose(s)),
      "Unknown purpose of processing"
    )

  def granularize(dc: Purpose): Set[Purpose] = {
    def granularize0(term: String): List[String] =
      val n = terms.filter(t => t.startsWith(s"$term."))
      if n.length == 0 then List(term) else n.flatMap(t => granularize0(t))

    val res =
      if dc.term == "*" then terms.tail.flatMap(t => granularize0(t))
      else granularize0(dc.term)

    res.toSet.map(Purpose(_))
  }

  val All = Purpose("*")

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

  def getAllPurposes = terms.map(Purpose(_)).toSet

  given Decoder[Purpose] =
    Decoder.decodeString.emap(Purpose.parse(_).toEither)

  given Encoder[Purpose] =
    Encoder[String].contramap(_.term)

  given Schema[Purpose] =
    Schema.string.validate(Validator.enumeration(terms.map(Purpose(_)), x => Option(x.term)))

}
