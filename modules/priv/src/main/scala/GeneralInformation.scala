package io.blindnet.pce
package priv

case class GeneralInformation(
    countries: List[String],
    organization: String,
    dpo: String,
    dataConsumerCategories: List[String],
    accessPolicies: List[String],
    privacyPolicyLink: Option[String],
    dataSecurityInfo: Option[String]
)
