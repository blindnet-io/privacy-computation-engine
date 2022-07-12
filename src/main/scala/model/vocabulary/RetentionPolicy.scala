package io.blindnet.privacy
package model.vocabulary

import model.vocabulary.terms.*

case class RetentionPolicy(
    policyType: RetentionTerms,
    // TODO: https://json-schema.org/draft/2020-12/json-schema-validation.html#name-dates-times-and-duration
    duration: String,
    after: EventTerms
)
