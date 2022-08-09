package io.blindnet.pce
package services

import cats.effect.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import api.endpoints.messages.privacyrequest.*
import model.error.*
import io.blindnet.pce.model.error.given
import io.blindnet.pce.util.extension.*
import java.util.*
import scala.util.*

object util {
  def validateUUID(s: String) =
    IO.fromTry(Try(UUID.fromString(s)))
      .as(s)
      .handleErrorWith(_ => s"Wrong id $s".failBadRequest)

  extension (s: String) {
    def failBadRequest = BadRequestException(BadPrivacyRequestPayload(s).asJson).raise
    def failNotFound   = NotFoundException(s).raise
  }

}
