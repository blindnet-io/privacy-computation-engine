package io.blindnet.pce
package services

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.kernel.Clock
import cats.effect.std.*
import cats.implicits.*
import io.blindnet.pce.api.endpoints.messages.consumerinterface.*
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
import priv.LegalBase
import priv.terms.EventTerms.*
import io.blindnet.pce.services.*
import testutil.*
import weaver.*
import io.blindnet.pce.priv.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import SharedResources.*
import testutil.*
import httputil.*

class UserEventsSuite(global: GlobalRead) extends IOSuite {

  type Res = Resources
  def sharedResource: Resource[IO, Resources] = global.getOrFailR[Resources]()

  val cId = UUID.fromString("28b5bee0-9db8-40ec-840e-64eafbfb9ddd")

  test("verifyLbExists should fail for non existing legal base") {
    res =>
      res.services.userEvents
        .verifyLbExists(appId, uuid, _.isConsent)
        .as(failure("should have failed"))
        .handleError(_ => success)
  }

  test("verifyLbExists should fail for wrong existing legal base") {
    res =>
      res.services.userEvents
        .verifyLbExists(appId, cId, _.isContract)
        .as(failure("should have failed"))
        .handleError(_ => success)
  }

  test("verifyLbExists should succeed") {
    res =>
      res.services.userEvents
        .verifyLbExists(appId, cId, _.isConsent)
        .as(success)
  }

  test("handleUser should register a new data subject if not known") {
    res =>
      for {
        ds <- UUIDGen.randomUUID[IO].map(id => DataSubject(id.toString, appId))
        _  <- res.services.userEvents.handleUser(appId, ds)
        // format: off
        userInDb <- sql"select exists (select id from data_subjects where id=${ds.id})".query[Boolean].unique.transact(res.xa)
        // format: on
        _ <- expect(userInDb).failFast
      } yield success
  }

}
