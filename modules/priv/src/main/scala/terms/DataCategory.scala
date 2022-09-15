package io.blindnet.pce
package priv
package terms

import cats.data.Validated
import io.circe.*
import sttp.tapir.{Schema, ValidationResult, Validator}

case class DataCategory(term: String)

object DataCategory {

  def parse(s: String): Validated[String, DataCategory] =
    Validated.fromOption(
      (terms.find(t => s.contains(t))).map(_ => DataCategory(s)),
      "Unknown data category"
    )

  def granularize(dc: DataCategory, selectors: Set[DataCategory] = Set.empty): Set[DataCategory] = {
    val allterms = terms ++ selectors.map(_.term)

    def granularize(term: String): List[String] =
      val n = allterms.filter(t => t.startsWith(s"$term."))
      if n.length == 0 then List(term) else n.flatMap(t => granularize(t))

    val res =
      if dc.term == "*" then allterms.tail.flatMap(t => granularize(t))
      else granularize(dc.term)

    res.toSet.map(DataCategory(_))
  }

  def exists(dc: DataCategory, selectors: Set[DataCategory]): Boolean =
    terms.contains(dc.term) || selectors.contains(dc)

  val All = DataCategory("*")

  val terms = List(
    "*",
    "AFFILIATION",
    "AFFILIATION.MEMBERSHIP",
    "AFFILIATION.MEMBERSHIP.UNION",
    "AFFILIATION.SCHOOL",
    "AFFILIATION.WORKPLACE",
    "BEHAVIOR",
    "BEHAVIOR.ACTIVITY",
    "BEHAVIOR.CONNECTION",
    "BEHAVIOR.PREFERENCE",
    "BEHAVIOR.TELEMETRY",
    "BIOMETRIC",
    "CONTACT",
    "CONTACT.EMAIL",
    "CONTACT.ADDRESS",
    "CONTACT.PHONE",
    "DEMOGRAPHIC",
    "DEMOGRAPHIC.AGE",
    "DEMOGRAPHIC.BELIEFS",
    "DEMOGRAPHIC.GENDER",
    "DEMOGRAPHIC.ORIGIN",
    "DEMOGRAPHIC.RACE",
    "DEMOGRAPHIC.SEXUAL-ORIENTATION",
    "DEVICE",
    "FINANCIAL",
    "FINANCIAL.BANK-ACCOUNT",
    "GENETIC",
    "HEALTH",
    "IMAGE",
    "LOCATION",
    "NAME",
    "PROFILING",
    "RELATIONSHIPS",
    "UID",
    "UID.ID",
    "UID.IP",
    "UID.USER-ACCOUNT",
    "UID.SOCIAL-MEDIA",
    "OTHER-DATA"
  )

  given Decoder[DataCategory] =
    Decoder.decodeString.emap(DataCategory.parse(_).toEither)

  given Encoder[DataCategory] =
    Encoder[String].contramap(_.term)

  given KeyEncoder[DataCategory] =
    KeyEncoder[String].contramap(_.term)

  given Schema[DataCategory] =
    Schema.string.validate(
      Validator.custom(s => ValidationResult.validWhen(terms.exists(s.term.startsWith)))
    )

}
