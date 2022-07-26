package io.blindnet.privacy
package model.vocabulary

import model.vocabulary.terms.*

case class PrivacyScopeTriple(
    dataCategories: DataCategory,
    processingCategories: ProcessingCategory,
    purpose: Purpose
)
