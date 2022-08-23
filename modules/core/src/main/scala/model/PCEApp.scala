package io.blindnet.pce
package model

import java.util.UUID
import org.http4s.Uri
import doobie.util.Read
import doobie.postgres.implicits.*

case class AutoResolve(
    transparency: Boolean,
    consents: Boolean,
    access: Boolean,
    delete: Boolean
)

case class PCEApp(
    id: UUID,
    dacUri: Uri,
    autoResolve: AutoResolve
)

object PCEApp {
  given Read[PCEApp] =
    Read[(UUID, String, Boolean, Boolean, Boolean, Boolean)]
      .map {
        case (id, dcaUri, t, c, a, d) =>
          PCEApp(id, Uri.unsafeFromString(dcaUri), AutoResolve(t, c, a, d))
      }

}
