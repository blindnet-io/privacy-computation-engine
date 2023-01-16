package io.blindnet.pce
package api.endpoints.messages.configuration

import java.util.UUID

import cats.effect.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*
import io.blindnet.pce.model.DacConfig

case class StorageConfigurationPayload(
    enabled: Boolean,
    uri: Option[String],
    token: Option[String]
)

object StorageConfigurationPayload {

  def fromDacConfig(dac: DacConfig) =
    StorageConfigurationPayload(dac.usingDac, dac.uri.map(_.toString), dac.token)

  given Decoder[StorageConfigurationPayload] = unSnakeCaseIfy(
    deriveDecoder[StorageConfigurationPayload]
  )

  given Encoder[StorageConfigurationPayload] = snakeCaseIfy(
    deriveEncoder[StorageConfigurationPayload]
  )

  given Schema[StorageConfigurationPayload] =
    Schema.derived[StorageConfigurationPayload](using
      Configuration.default.withSnakeCaseMemberNames
    )

}
