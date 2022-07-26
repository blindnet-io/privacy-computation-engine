package io.blindnet.privacy
package model.vocabulary.request

import model.vocabulary.terms.*

trait Restriction

case class PrivacyScopeRestriction(
    triples: List[(DataCategory, ProcessingCategory, Purpose)]
) extends Restriction

case class ConsentRestriction(consentId: String) extends Restriction

// TODO: dates
case class DateRangeRestriction(
    from: Option[String],
    to: Option[String]
) extends Restriction

case class ProvenanceRestriction(
    provenanceCategory: ProvenanceTerms,
    target: Target
) extends Restriction

case class DataReferenceRestriction(dataReferences: List[String]) extends Restriction
