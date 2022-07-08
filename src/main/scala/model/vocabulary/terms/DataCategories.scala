package io.blindnet.privacy
package model.vocabulary.terms

case class DataCategory(term: String)

object DataCategory {

  def parse(s: String): Option[DataCategory] =
    s.split(".").headOption.filter(terms.contains).map(_ => DataCategory(s))

  val terms = List(
    "*",
    "AFFILIATION",
    // "AFFILIATION.MEMBERSHIP",
    // "AFFILIATION.MEMBERSHIP.UNION",
    // "AFFILIATION.SCHOOL",
    // "AFFILIATION.WORKPLACE",
    "BEHAVIOR",
    // "BEHAVIOR.ACTIVITY",
    // "BEHAVIOR.CONNECTION",
    // "BEHAVIOR.PREFERENCE",
    // "BEHAVIOR.TELEMETRY",
    "BIOMETRIC",
    "CONTACT",
    // "CONTACT.EMAIL",
    // "CONTACT.ADDRESS",
    // "CONTACT.PHONE",
    "DEMOGRAPHIC",
    // "DEMOGRAPHIC.AGE",
    // "DEMOGRAPHIC.BELIEFS",
    // "DEMOGRAPHIC.GENDER",
    // "DEMOGRAPHIC.ORIGIN",
    // "DEMOGRAPHIC.RACE",
    // "DEMOGRAPHIC.SEXUAL-ORIENTATION",
    "DEVICE",
    "FINANCIAL",
    // "FINANCIAL.BANK-ACCOUNT",
    "GENETIC",
    "HEALTH",
    "IMAGE",
    "LOCATION",
    "NAME",
    "PROFILING",
    "RELATIONSHIPS",
    "UID",
    // "UID.ID",
    // "UID.IP",
    // "UID.USER-ACCOUNT ",
    // "UID.SOCIAL-MEDIA ",
    "OTHER-DATA"
  )

}
