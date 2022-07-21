package io.blindnet.privacy
package model.vocabulary.terms

import cats.data.Validated
import io.circe.*

case class DataCategory(term: String)

object DataCategory {

  def parse(s: String): Validated[String, DataCategory] =
    Validated.fromOption(
      s.split('.').headOption.filter(terms.contains).map(_ => DataCategory(s)),
      "Unknown data category"
    )

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
    "UID.USER-ACCOUNT ",
    "UID.SOCIAL-MEDIA ",
    "OTHER-DATA"
  )

  given Decoder[DataCategory] =
    Decoder.decodeString.emap(DataCategory.parse(_).toEither)

  given Encoder[DataCategory] =
    Encoder[String].contramap(_.term)

}
