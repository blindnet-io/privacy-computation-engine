package io.blindnet.pce
package priv

import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.*
import sttp.tapir.generic.Configuration

case class GeneralInformation(
    @description(
      "countries where data servers are located (including those of the processors who are processing data on your behalf"
    )
    @encodedExample(List("France", "USA"))
    countries: List[String],
    @description("name and contact details of your Organization and its representative")
    @encodedExample("blindnet")
    organization: String,
    @description(
      "identity and contact of a Data Protection Officer - if you are using blindnet devkit Privacy Request Builder, include the URL where you are hosting the interface"
    )
    @encodedExample("Vuk Janosevic, www.blindnet.io/privacy-request-builder")
    dpo: String,
    @encodedExample("Blindnet account managers, and Blindnet's DPO")
    dataConsumerCategories: List[String],
    accessPolicies: List[String],
    @description("public URL where your Privacy Policy can be consulted")
    @encodedExample("https://blindnet.io/privacy")
    privacyPolicyLink: Option[String],
    @description(
      "general description of the technical and organizational security measures referred to in Article 32 of GDPR"
    )
    @encodedExample(
      "We use administrative, technical, and physical safeguards to protect your personal data, taking into account the nature of the personal data and the processing, and the threats posed."
    )
    dataSecurityInfo: Option[String]
)

object GeneralInformation {
  given Schema[GeneralInformation] =
    Schema.derived[GeneralInformation](using Configuration.default.withSnakeCaseMemberNames)

}
