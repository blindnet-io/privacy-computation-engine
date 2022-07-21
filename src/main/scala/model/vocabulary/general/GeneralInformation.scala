package io.blindnet.privacy
package model.vocabulary.general

case class GeneralInformation(
    countries: List[String],
    organizations: List[Organization],
    dpo: List[Dpo],
    dataConsumerCategories: List[String],
    accessPolicies: List[String],
    privacyPolicyLink: Option[String],
    dataSecurityInfo: Option[String]
)
