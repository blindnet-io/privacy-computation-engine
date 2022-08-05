package io.blindnet.privacy
package model.vocabulary

import model.vocabulary.terms.*

case class LegalBase(
    lbType: LegalBaseTerms,
    scope: PrivacyScope,
    name: Option[String] = None,
    description: Option[String] = None,
    active: Boolean
)
