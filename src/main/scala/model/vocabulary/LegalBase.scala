package io.blindnet.privacy
package model.vocabulary

import model.vocabulary.terms.*

case class LegalBase(
    term: LegalBaseTerms,
    scope: List[PrivacyScopeTriple],
    name: Option[String] = None,
    description: Option[String] = None,
    active: Boolean
)
