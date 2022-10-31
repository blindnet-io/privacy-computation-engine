package io.blindnet.pce
package services

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.implicits.*
import cats.effect.kernel.Clock
import cats.effect.std.*
import cats.implicits.*
import io.blindnet.identityclient.auth.*
import io.blindnet.pce.model.*
import io.blindnet.pce.model.error.given
import io.blindnet.pce.util.extension.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import api.endpoints.messages.privacyrequest.*
import db.repositories.*
import model.error.*
import priv.DataSubject
import priv.privacyrequest.{ Demand, PrivacyRequest, * }
import priv.terms.*
import util.*

class UserService(
    repos: Repositories
) {
  def getGivenConsents(jwt: UserJwt)(x: Unit) =
    for {
      timeline <- repos.events.getTimelineNoScope(DataSubject(jwt.userId, jwt.appId))
      givenConsents = timeline.getConsentGivenEvents()
      res <- givenConsents.toNel match {
        case None      => IO(List.empty)
        case Some(gcs) =>
          for {
            lbEvents <- repos.legalBase.get(jwt.appId, gcs.map(_.lbId))
            res = lbEvents.map(
              e => GivenConsentsPayload(e.id, e.name, gcs.find(_.lbId == e.id).map(_.timestamp).get)
            )
          } yield res
      }
    } yield res

}
