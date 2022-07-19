package io.blindnet.privacy
package model.vocabulary.terms

case class Purpose(term: String)

object Purpose {

  def parse(s: String): Option[Purpose] =
    s.split(".").headOption.filter(terms.contains).map(_ => Purpose(s))

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

}
