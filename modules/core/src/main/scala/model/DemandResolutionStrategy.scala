package io.blindnet.pce
package model

import java.util.UUID

import doobie.postgres.implicits.*
import org.http4s.Uri
import cats.implicits.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.generic.Configuration

enum DemandResolution {
  case Automatic
  case Manual

  def encode = this match {
    case Automatic => "auto"
    case Manual    => "manual"
  }

}

object DemandResolution {
  given Decoder[DemandResolution] =
    Decoder.decodeString.emap {
      case "auto"   => Automatic.asRight
      case "manual" => Manual.asRight
      case _        => "Unknown resolution strategy".asLeft
    }

  given Encoder[DemandResolution] =
    Encoder[String].contramap(_.encode)

  given Schema[DemandResolution] =
    Schema.derivedEnumeration[DemandResolution](encode = Some(_.encode))

}

case class DemandResolutionStrategy(
    transparency: DemandResolution,
    access: DemandResolution,
    delete: DemandResolution,
    revokeConsent: DemandResolution,
    objectScope: DemandResolution,
    restrictScope: DemandResolution
) {
  val isAutoTransparency  = transparency == DemandResolution.Automatic
  val isAutoAccess        = access == DemandResolution.Automatic
  val isAutoDelete        = delete == DemandResolution.Automatic
  val isAutoRevokeConsent = revokeConsent == DemandResolution.Automatic
  val isAutoObject        = objectScope == DemandResolution.Automatic
  val isAutoRestrict      = restrictScope == DemandResolution.Automatic
}

object DemandResolutionStrategy {

  def simple(t: Boolean, a: Boolean, d: Boolean, rc: Boolean, ob: Boolean, re: Boolean) =
    import DemandResolution.*
    DemandResolutionStrategy(
      transparency = if t then Automatic else Manual,
      access = if a then Automatic else Manual,
      delete = if d then Automatic else Manual,
      revokeConsent = if rc then Automatic else Manual,
      objectScope = if ob then Automatic else Manual,
      restrictScope = if re then Automatic else Manual
    )

  given Decoder[DemandResolutionStrategy] =
    unSnakeCaseIfy(
      Decoder.forProduct6(
        "transparency",
        "access",
        "delete",
        "revoke_consent",
        "object",
        "restrict"
      )(
        DemandResolutionStrategy.apply
      )
    )

  given Encoder[DemandResolutionStrategy] =
    snakeCaseIfy(
      Encoder.forProduct6(
        "transparency",
        "access",
        "delete",
        "revoke_consent",
        "object",
        "restrict"
      )(s => (s.transparency, s.access, s.delete, s.revokeConsent, s.objectScope, s.restrictScope))
    )

  given Schema[DemandResolutionStrategy] =
    Schema.derived[DemandResolutionStrategy](using Configuration.default.withSnakeCaseMemberNames)

}
