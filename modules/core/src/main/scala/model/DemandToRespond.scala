package io.blindnet.pce
package model

import java.util.UUID
import org.http4s.Uri
import doobie.util.Read
import doobie.postgres.implicits.*
import io.circe.Json

case class DemandToRespond(
    dId: UUID,
    data: Json = Json.Null
)

object DemandToRespond {}
