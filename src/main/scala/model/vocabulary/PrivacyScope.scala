package io.blindnet.privacy
package model.vocabulary

import model.vocabulary.terms.*

case class PrivacyScope(
    dataCategories: List[DataCategory],
    processingCategories: List[ProcessingCategory],
    purpose: List[Purpose]
)
