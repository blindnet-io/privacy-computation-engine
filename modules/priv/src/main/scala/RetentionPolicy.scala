package io.blindnet.pce
package priv

import terms.*

case class RetentionPolicy(
    policyType: RetentionPolicyTerms,
    // TODO: https://json-schema.org/draft/2020-12/json-schema-validation.html#name-dates-times-and-duration
    duration: String,
    after: EventTerms
)
