package io.blindnet.privacy
package model.vocabulary.general

import io.blindnet.model.vocabulary.general.*

case class GeneralInformation(
    countries: List[String],
    organizations: List[Organization],
    dpo: List[Dpo],
    dataConsumerCategories: List[String],
    accessPolicies: List[String],
    privacyPolicyLink: String
)
