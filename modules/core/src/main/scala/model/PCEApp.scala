package io.blindnet.pce
package model

import java.util.UUID

import doobie.postgres.implicits.*
import doobie.util.Read
import org.http4s.Uri

case class DacConfig(
    usingDac: Boolean,
    uri: Option[Uri],
    token: Option[String]
)

case class PCEApp(
    id: UUID,
    dac: DacConfig,
    resolutionStrategy: DemandResolutionStrategy
)

object PCEApp {
  given Read[PCEApp] =
    // format: off
    Read[(UUID, Boolean, Option[String], Option[String], Boolean, Boolean, Boolean, Boolean, Boolean, Boolean)]
    // format: on
      .map {
        case (id, usingDac, dacUri, dacToken, t, a, d, rc, ob, re) =>
          PCEApp(
            id,
            DacConfig(usingDac, dacUri.map(Uri.unsafeFromString), dacToken),
            DemandResolutionStrategy.simple(t, a, d, rc, ob, re)
          )
      }

}
