package io.blindnet.privacy
package model.vocabulary

import model.vocabulary.terms.*
import model.vocabulary.terms.DataCategory
import java.time.Instant

case class Recommendation(
    id: String,
    dId: String,
    dataCategories: Set[DataCategory],
    dateFrom: Option[Instant],
    dateTo: Option[Instant],
    provenance: Option[ProvenanceTerms]
)
