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

case class DacConfig(
    usingDac: Boolean,
    uri: Option[Uri]
)

case class PCEApp(
    id: UUID,
    dac: DacConfig,
    autoResolve: AutoResolve
)

object PCEApp {
  given Read[PCEApp] =
    Read[(UUID, Boolean, Option[String], Boolean, Boolean, Boolean, Boolean)]
      .map {
        case (id, usingDac, dacUri, t, c, a, d) =>
          PCEApp(
            id,
            DacConfig(usingDac, dacUri.map(Uri.unsafeFromString)),
            AutoResolve(t, c, a, d)
          )
      }

}
