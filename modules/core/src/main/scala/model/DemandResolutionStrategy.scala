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

  given Decoder[DemandResolutionStrategy] = deriveDecoder[DemandResolutionStrategy]
  given Encoder[DemandResolutionStrategy] = deriveEncoder[DemandResolutionStrategy]

  given Schema[DemandResolutionStrategy] = Schema.derived[DemandResolutionStrategy]

}
