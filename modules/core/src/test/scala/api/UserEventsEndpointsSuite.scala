package io.blindnet.pce
package api

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
import util.*
import priv.LegalBase
import io.blindnet.pce.services.*
import testutil.*
import weaver.*
import io.blindnet.pce.priv.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import io.circe.*
import io.circe.parser.*
import io.circe.literal.*
import httputil.*

object UserEventsEndpointsSuite extends FuncSuite {

  val cId  = UUID.fromString("28b5bee0-9db8-40ec-840e-64eafbfb9ddd")
  val ctId = UUID.fromString("87630175-e5ff-4f71-9fba-8f1851ff5dca")

  test("fail recording consent given for wrong consent id") {
    res =>
      val req = json"""{ "data_subject": {"id": ${ds.id}}, "consent_id": $uuid, "date": $now }"""
      res.server
        .run(post("user-events/consent", req))
        .map(response => expect(response.status == Status.NotFound))
  }

  test("record consent given for known user") {
    res =>
      val req = json"""{ "data_subject": {"id": ${ds.id}}, "consent_id": $cId, "date": $now }"""
      for {
        response <- res.server.run(post("user-events/consent", req))
        _        <- expect(response.status == Status.Ok).failFast
        sql =
          sql"select exists (select lbid from consent_given_events where lbid=$cId and dsid=${ds.id})"
        exists <- sql.query[Boolean].unique.transact(res.xa)
        _      <- expect(exists).failFast
      } yield success
  }

  test("record consent given for unknown user") {
    res =>
      val uid = uuid.toString
      val req = json"""{ "data_subject": {"id": $uid}, "consent_id": $cId, "date": $now }"""
      for {
        response <- res.server.run(post("user-events/consent", req))
        _        <- expect(response.status == Status.Ok).failFast

        sqlEv =
          sql"select exists (select lbid from consent_given_events where lbid=$cId and dsid=$uid)"
        existsEv <- sqlEv.query[Boolean].unique.transact(res.xa)
        _        <- expect(existsEv).failFast

        sqlDs = sql"select exists (select id from data_subjects where id=$uid)"
        existsDs <- sqlDs.query[Boolean].unique.transact(res.xa)
        _        <- expect(existsDs).failFast

      } yield success
  }

  test("fail recording start of contract for wrong contract id") {
    res =>
      val req = json"""{ "data_subject": {"id": ${ds.id}}, "contract_id": $uuid, "date": $now }"""
      res.server
        .run(post("user-events/contract/start", req))
        .map(response => expect(response.status == Status.NotFound))
  }

  test("record start of contract for known user") {
    res =>
      val req = json"""{ "data_subject": {"id": ${ds.id}}, "contract_id": $ctId, "date": $now }"""
      for {
        response <- res.server.run(post("user-events/contract/start", req))
        _        <- expect(response.status == Status.Ok).failFast
        sql =
          sql"select exists (select lbid from legal_base_events where lbid=$ctId and dsid=${ds.id})"
        exists <- sql.query[Boolean].unique.transact(res.xa)
        _      <- expect(exists).failFast
      } yield success
  }

  test("record start of contract for unknown user") {
    res =>
      val uid = uuid.toString
      val req = json"""{ "data_subject": {"id": $uid}, "contract_id": $ctId, "date": $now }"""
      for {
        response <- res.server.run(post("user-events/contract/start", req))
        _        <- expect(response.status == Status.Ok).failFast

        sqlEv =
          sql"select exists (select lbid from legal_base_events where lbid=$ctId and dsid=$uid)"
        existsEv <- sqlEv.query[Boolean].unique.transact(res.xa)
        _        <- expect(existsEv).failFast

        sqlDs = sql"select exists (select id from data_subjects where id=$uid)"
        existsDs <- sqlDs.query[Boolean].unique.transact(res.xa)
        _        <- expect(existsDs).failFast

      } yield success
  }

}
