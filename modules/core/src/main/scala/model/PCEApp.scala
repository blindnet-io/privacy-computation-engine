package io.blindnet.pce
package model

import java.util.UUID
import org.http4s.Uri
import doobie.util.Read
import doobie.postgres.implicits.*

case class PCEApp(
    id: UUID,
    dacUri: Uri
)

object PCEApp {
  given Read[PCEApp] =
    Read[(UUID, String)]
      .map { case (id, dcaUri) => PCEApp(id, Uri.unsafeFromString(dcaUri)) }

}
