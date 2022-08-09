package io.blindnet.pce
package priv

import terms.*

case class LegalBase(
    lbType: LegalBaseTerms,
    scope: PrivacyScope,
    name: Option[String] = None,
    description: Option[String] = None,
    active: Boolean
)
