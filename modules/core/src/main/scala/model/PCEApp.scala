package io.blindnet.pce
package model

import java.util.UUID

import doobie.postgres.implicits.*
import doobie.util.Read
import org.http4s.Uri

enum DemandResolution {
  case Automatic
  case Manual
}

case class DemandResolutionStrategy(
    transparency: DemandResolution,
    access: DemandResolution,
    delete: DemandResolution,
    consents: DemandResolution
) {
  val isAutoTransparency = transparency == DemandResolution.Automatic
  val isAutoAccess       = access == DemandResolution.Automatic
  val isAutoDelete       = delete == DemandResolution.Automatic
  val isAutoConsents     = consents == DemandResolution.Automatic
}

object DemandResolutionStrategy {

  def simple(t: Boolean, a: Boolean, d: Boolean, c: Boolean) =
    import DemandResolution.*
    DemandResolutionStrategy(
      transparency = if t then Automatic else Manual,
      access = if a then Automatic else Manual,
      delete = if d then Automatic else Manual,
      consents = if c then Automatic else Manual
    )

}

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
