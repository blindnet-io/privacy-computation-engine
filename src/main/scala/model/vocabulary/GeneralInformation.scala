package io.blindnet.privacy
package model.vocabulary

case class GeneralInformation(
    countries: List[String],
    organization: String,
    dpo: String,
    dataConsumerCategories: List[String],
    accessPolicies: List[String],
    privacyPolicyLink: Option[String],
    dataSecurityInfo: Option[String]
)
