package io.blindnet.privacy
package model.vocabulary

import model.vocabulary.terms.*

case class Selector(
    id: String,
    name: String,
    dataCategory: DataCategory,
    provenance: Provenance
)
