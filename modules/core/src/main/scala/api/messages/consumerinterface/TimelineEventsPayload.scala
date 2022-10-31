package io.blindnet.pce
package api.endpoints.messages.consumerinterface

import java.time.Instant
import java.util.UUID

import cats.effect.*
import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import sttp.tapir.*
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import priv.*
import priv.privacyrequest.*
import priv.terms.*
import api.endpoints.messages.*

case class PrivacyRequetsDemand(
    id: UUID,
    action: Action
)

case class PrivacyRequestEvent(
    id: UUID,
    date: Instant,
    target: Target,
    demands: List[PrivacyRequetsDemand]
)

case class PrivacyResponseEvent(
    id: UUID,
    demandId: UUID,
    action: Action,
    includes: List[Json],
    date: Instant,
    status: Status
)

case class GivenConsentEvent(
    id: UUID,
    date: Instant,
    name: Option[String],
    scope: PrivacyScope
)

case class RevokedConsentEvent(
    id: UUID,
    date: Instant,
    name: Option[String]
)

case class LegalBaseEvent(
    id: UUID,
    date: Instant,
    event: EventTerms,
    `type`: LegalBaseTerms,
    name: Option[String],
    scope: PrivacyScope
)

case class TimelineEventsPayload(
    requests: List[PrivacyRequestEvent],
    responses: List[PrivacyResponseEvent],
    givenConsents: List[GivenConsentEvent],
    revokedConsents: List[RevokedConsentEvent],
    legalBases: List[LegalBaseEvent]
)

object TimelineEventsPayload {
  given Decoder[PrivacyRequetsDemand] = unSnakeCaseIfy(deriveDecoder[PrivacyRequetsDemand])
  given Encoder[PrivacyRequetsDemand] = snakeCaseIfy(deriveEncoder[PrivacyRequetsDemand])
  given Schema[PrivacyRequetsDemand]  =
    Schema.derived[PrivacyRequetsDemand](using Configuration.default.withSnakeCaseMemberNames)

  given Decoder[PrivacyRequestEvent] = unSnakeCaseIfy(deriveDecoder[PrivacyRequestEvent])
  given Encoder[PrivacyRequestEvent] = snakeCaseIfy(deriveEncoder[PrivacyRequestEvent])
  given Schema[PrivacyRequestEvent]  =
    Schema.derived[PrivacyRequestEvent](using Configuration.default.withSnakeCaseMemberNames)

  given Decoder[PrivacyResponseEvent] = unSnakeCaseIfy(deriveDecoder[PrivacyResponseEvent])
  given Encoder[PrivacyResponseEvent] = snakeCaseIfy(deriveEncoder[PrivacyResponseEvent])
  given Schema[PrivacyResponseEvent]  =
    Schema.derived[PrivacyResponseEvent](using Configuration.default.withSnakeCaseMemberNames)

  given Decoder[GivenConsentEvent] = unSnakeCaseIfy(deriveDecoder[GivenConsentEvent])
  given Encoder[GivenConsentEvent] = snakeCaseIfy(deriveEncoder[GivenConsentEvent])
  given Schema[GivenConsentEvent]  =
    Schema.derived[GivenConsentEvent](using Configuration.default.withSnakeCaseMemberNames)

  given Decoder[RevokedConsentEvent] = unSnakeCaseIfy(deriveDecoder[RevokedConsentEvent])
  given Encoder[RevokedConsentEvent] = snakeCaseIfy(deriveEncoder[RevokedConsentEvent])
  given Schema[RevokedConsentEvent]  =
    Schema.derived[RevokedConsentEvent](using Configuration.default.withSnakeCaseMemberNames)

  given Decoder[LegalBaseEvent] = unSnakeCaseIfy(deriveDecoder[LegalBaseEvent])
  given Encoder[LegalBaseEvent] = snakeCaseIfy(deriveEncoder[LegalBaseEvent])
  given Schema[LegalBaseEvent]  =
    Schema.derived[LegalBaseEvent](using Configuration.default.withSnakeCaseMemberNames)

  given Decoder[TimelineEventsPayload] = unSnakeCaseIfy(deriveDecoder[TimelineEventsPayload])
  given Encoder[TimelineEventsPayload] = snakeCaseIfy(deriveEncoder[TimelineEventsPayload])
  given Schema[TimelineEventsPayload]  =
    Schema.derived[TimelineEventsPayload](using Configuration.default.withSnakeCaseMemberNames)

  def fromPriv(
      reqs: List[PrivacyRequest],
      resps: List[PrivacyResponse],
      timeline: Timeline,
      lbs: List[LegalBase]
  ) = {
    val requests = reqs.map(
      r =>
        PrivacyRequestEvent(
          r.id.value,
          r.timestamp,
          r.target,
          r.demands.map(d => PrivacyRequetsDemand(d.id, d.action))
        )
    )

    def mapResponse(r: PrivacyResponse): PrivacyResponseEvent =
      PrivacyResponseEvent(
        r.id.value,
        r.demandId,
        r.action,
        r.includes.map(rr => mapResponse(rr).asJson),
        r.timestamp,
        r.status
      )

    val responses = resps.map(mapResponse)

    def getLbName(id: UUID) =
      lbs.find(_.id == id).flatMap(_.name)

    val givenConsents   =
      timeline.givenConsents.map(
        e => GivenConsentEvent(e.lbId, e.timestamp, getLbName(e.lbId), e.scope)
      )
    val revokedConsents =
      timeline.revokedConsents.map(e => RevokedConsentEvent(e.lbId, e.timestamp, getLbName(e.lbId)))
    val legalBases      =
      timeline.legalBases.map(
        e => LegalBaseEvent(e.lbId, e.timestamp, e.eType, e.lb, getLbName(e.lbId), e.scope)
      )

    TimelineEventsPayload(requests, responses, givenConsents, revokedConsents, legalBases)
  }

}
