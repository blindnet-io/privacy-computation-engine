package io.blindnet.pce
package model

import java.util.UUID

import doobie.postgres.implicits.*
import doobie.util.Read
import org.http4s.Uri

case class DacConfig(
    usingDac: Boolean,
    uri: Option[Uri]
)

case class PCEApp(
    id: UUID,
    dac: DacConfig,
    resolutionStrategy: DemandResolutionStrategy
)

object PCEApp {
  given Read[PCEApp] =
    Read[(UUID, Boolean, Option[String], Boolean, Boolean, Boolean, Boolean)]
      .map {
        case (id, usingDac, dacUri, t, a, d, c) =>
          PCEApp(
            id,
            DacConfig(usingDac, dacUri.map(Uri.unsafeFromString)),
            DemandResolutionStrategy.simple(t, a, d, c)
          )
      }

}
