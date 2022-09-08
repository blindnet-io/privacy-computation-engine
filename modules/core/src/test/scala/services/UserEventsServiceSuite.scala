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

object UserEventsSuite extends FuncSuite {

  val cId = UUID.fromString("28b5bee0-9db8-40ec-840e-64eafbfb9ddd")

  test("verifyLbExists should fail for non existent legal base") {
    res =>
      res.services.userEvents
        .verifyLbExists(appId, uuid, _.isConsent)
        .as(failure("should have failed"))
        .handleError(_ => success)
  }

  test("verifyLbExists should fail for wrong existent legal base") {
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

}
