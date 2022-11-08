package io.blindnet.pce
package api

import java.time.Instant
import java.util.UUID

import cats.data.{ NonEmptyList, * }
import cats.effect.*
import cats.effect.kernel.Clock
import cats.effect.std.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import io.blindnet.pce.api.endpoints.messages.consumerinterface.*
import io.blindnet.pce.priv.*
import io.blindnet.pce.services.*
import io.blindnet.pce.util.*
import io.blindnet.pce.util.extension.*
import io.circe.generic.auto.*
import io.circe.literal.*
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.{ Json, * }
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import weaver.*
import api.endpoints.messages.privacyrequest.*
import db.repositories.*
import model.error.*
import priv.DataSubject
import priv.privacyrequest.{ Demand, PrivacyRequest, * }
import priv.LegalBase
import testutil.*
import httputil.*

object UserEventsEndpointsSuite extends FuncSuite {

  val consent1  = "28b5bee0-9db8-40ec-840e-64eafbfb9ddd".uuid
  val consent2  = "b25c1c0c-d375-4a5c-8500-6918f2888435".uuid
  val consent3  = "b52f8b4b-590c-4dcb-b572-f4a890ea330b".uuid
  val contract1 = "0e3bcc80-09a0-45c2-9e3f-454f953e3cfb".uuid
  val legInter1 = "db8db4ab-0ac2-4528-a333-576e8d0e10fe".uuid

  test("fail recording given consent for unknown consent id") {
    res =>
      val req = json"""{ "consent_id": $uuid }"""
      res.server
        .run(post("user-events/consent", req, Some(userToken)))
        .map(response => expect(response.status == Status.NotFound))
  }

  test("record given consent for known user") {
    res =>
      val req = json"""{ "consent_id": $consent1 }"""
      for {
        response <- res.server.run(post("user-events/consent", req, Some(userToken)))
        _        <- expect(response.status == Status.Ok).failFast
        sql =
          sql"select exists (select lbid from consent_given_events where lbid=$consent1 and dsid=${ds.id})"
        userInDb <- sql.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast
      } yield success
  }

  test("record given consent for unknown user") {
    res =>
      val uid   = uuid.toString
      val token = tb.user(uid)
      val req   = json"""{ "consent_id": $consent1 }"""
      for {
        response <- res.server.run(post("user-events/consent", req, Some(token)))
        _        <- expect(response.status == Status.Ok).failFast

        sqlEv =
          sql"select exists (select lbid from consent_given_events where lbid=$consent1 and dsid=$uid)"
        existsEv <- sqlEv.query[Boolean].unique.transact(res.xa)
        _        <- expect(existsEv).failFast

        sqlDs = sql"select exists (select id from data_subjects where id=$uid)"
        userInDb <- sqlDs.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast

      } yield success
  }

  test("fail storing consent for unkwnown consent id") {
    res =>
      val req = json"""
      {
          "dataSubject": {"id": ${ds.id}},
          "consentId": $uuid,
          "date": $now
      }
      """
      res.server
        .run(post("user-events/consent/store", req, Some(appToken)))
        .map(response => expect(response.status == Status.NotFound))
  }

  test("store given consent for known user") {
    res =>
      val req = json"""
      {
          "dataSubject": {"id": ${ds.id}},
          "consentId": $consent2,
          "date": $now
      }
      """
      for {
        response <- res.server.run(post("user-events/consent/store", req, Some(appToken)))
        _        <- expect(response.status == Status.Ok).failFast
        sql =
          sql"select exists (select lbid from consent_given_events where lbid=$consent2 and dsid=${ds.id})"
        userInDb <- sql.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast
      } yield success
  }

  test("store given consent for unknown user") {
    res =>
      val uid = uuid.toString
      val req = json"""
      {
          "dataSubject": {"id": $uid},
          "consentId": $consent2,
          "date": $now
      }
      """
      for {
        response <- res.server.run(post("user-events/consent/store", req, Some(appToken)))
        _        <- expect(response.status == Status.Ok).failFast

        sqlEv =
          sql"select exists (select lbid from consent_given_events where lbid=$consent2 and dsid=$uid)"
        existsEv <- sqlEv.query[Boolean].unique.transact(res.xa)
        _        <- expect(existsEv).failFast

        sqlDs = sql"select exists (select id from data_subjects where id=$uid)"
        userInDb <- sqlDs.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast

      } yield success
  }

  test("fail recording start of contract for unknown contract id") {
    res =>
      val req = json"""{ "data_subject": {"id": ${ds.id}}, "contract_id": $uuid, "date": $now }"""
      res.server
        .run(post("user-events/contract/start", req, Some(appToken)))
        .map(response => expect(response.status == Status.NotFound))
  }

  test("store start of contract known user") {
    res =>
      val req =
        json"""{ "data_subject": {"id": ${ds.id}}, "contract_id": $contract1, "date": $now }"""
      for {
        response <- res.server.run(post("user-events/contract/start", req, Some(appToken)))
        _        <- expect(response.status == Status.Ok).failFast
        sql =
          sql"select exists (select lbid from legal_base_events where lbid=$contract1 and dsid=${ds.id} and event='SERVICE-START')"
        userInDb <- sql.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast
      } yield success
  }

  test("store start of contract for unknown user") {
    res =>
      val uid = uuid.toString
      val req =
        json"""{ "data_subject": {"id": $uid}, "contract_id": $contract1, "date": $now }"""
      for {
        response <- res.server.run(post("user-events/contract/start", req, Some(appToken)))
        _        <- expect(response.status == Status.Ok).failFast

        sqlEv =
          sql"select exists (select lbid from legal_base_events where lbid=$contract1 and dsid=$uid and event='SERVICE-START')"
        existsEv <- sqlEv.query[Boolean].unique.transact(res.xa)
        _        <- expect(existsEv).failFast

        sqlDs = sql"select exists (select id from data_subjects where id=$uid)"
        userInDb <- sqlDs.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast

      } yield success
  }

  test("fail recording end of contract for unknown contract id") {
    res =>
      val req = json"""{ "data_subject": {"id": ${ds.id}}, "contract_id": $uuid, "date": $now }"""
      res.server
        .run(post("user-events/contract/end", req, Some(appToken)))
        .map(response => expect(response.status == Status.NotFound))
  }

  test("store end of contract known user") {
    res =>
      val req =
        json"""{ "data_subject": {"id": ${ds.id}}, "contract_id": $contract1, "date": $now }"""
      for {
        response <- res.server.run(post("user-events/contract/end", req, Some(appToken)))
        _        <- expect(response.status == Status.Ok).failFast
        sql =
          sql"select exists (select lbid from legal_base_events where lbid=$contract1 and dsid=${ds.id} and event='SERVICE-END')"
        userInDb <- sql.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast
      } yield success
  }

  test("store end of contract for unknown user") {
    res =>
      val uid = uuid.toString
      val req =
        json"""{ "data_subject": {"id": $uid}, "contract_id": $contract1, "date": $now }"""
      for {
        response <- res.server.run(post("user-events/contract/end", req, Some(appToken)))
        _        <- expect(response.status == Status.Ok).failFast

        sqlEv =
          sql"select exists (select lbid from legal_base_events where lbid=$contract1 and dsid=$uid and event='SERVICE-END')"
        existsEv <- sqlEv.query[Boolean].unique.transact(res.xa)
        _        <- expect(existsEv).failFast

        sqlDs = sql"select exists (select id from data_subjects where id=$uid)"
        userInDb <- sqlDs.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast

      } yield success
  }

  test("fail recording start of legitimate interest for unknown legitimate interest id") {
    res =>
      val req =
        json"""{ "data_subject": {"id": ${ds.id}}, "legitimate_interest_id": $uuid, "date": $now }"""
      res.server
        .run(post("user-events/legitimate-interest/start", req, Some(appToken)))
        .map(response => expect(response.status == Status.NotFound))
  }

  test("store start of legitimate interest known user") {
    res =>
      val req =
        json"""{ "data_subject": {"id": ${ds.id}}, "legitimate_interest_id": $legInter1, "date": $now }"""
      for {
        response <- res.server.run(
          post("user-events/legitimate-interest/start", req, Some(appToken))
        )
        _        <- expect(response.status == Status.Ok).failFast
        sql =
          sql"select exists (select lbid from legal_base_events where lbid=$legInter1 and dsid=${ds.id} and event='SERVICE-START')"
        userInDb <- sql.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast
      } yield success
  }

  test("store start of legitimate interest for unknown user") {
    res =>
      val uid = uuid.toString
      val req =
        json"""{ "data_subject": {"id": $uid}, "legitimate_interest_id": $legInter1, "date": $now }"""
      for {
        response <- res.server.run(
          post("user-events/legitimate-interest/start", req, Some(appToken))
        )
        _        <- expect(response.status == Status.Ok).failFast

        sqlEv =
          sql"select exists (select lbid from legal_base_events where lbid=$legInter1 and dsid=$uid and event='SERVICE-START')"
        existsEv <- sqlEv.query[Boolean].unique.transact(res.xa)
        _        <- expect(existsEv).failFast

        sqlDs = sql"select exists (select id from data_subjects where id=$uid)"
        userInDb <- sqlDs.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast

      } yield success
  }

  test("fail recording end of legitimate interest for unknown contract id") {
    res =>
      val req =
        json"""{ "data_subject": {"id": ${ds.id}}, "legitimate_interest_id": $uuid, "date": $now }"""
      res.server
        .run(post("user-events/legitimate-interest/end", req, Some(appToken)))
        .map(response => expect(response.status == Status.NotFound))
  }

  test("store end of legitimate interest known user") {
    res =>
      val req =
        json"""{ "data_subject": {"id": ${ds.id}}, "legitimate_interest_id": $legInter1, "date": $now }"""
      for {
        response <- res.server.run(post("user-events/legitimate-interest/end", req, Some(appToken)))
        _        <- expect(response.status == Status.Ok).failFast
        sql =
          sql"select exists (select lbid from legal_base_events where lbid=$legInter1 and dsid=${ds.id} and event='SERVICE-END')"
        userInDb <- sql.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast
      } yield success
  }

  test("store end of legitimate interest for unknown user") {
    res =>
      val uid = uuid.toString
      val req =
        json"""{ "data_subject": {"id": $uid}, "legitimate_interest_id": $legInter1, "date": $now }"""
      for {
        response <- res.server.run(post("user-events/legitimate-interest/end", req, Some(appToken)))
        _        <- expect(response.status == Status.Ok).failFast

        sqlEv =
          sql"select exists (select lbid from legal_base_events where lbid=$legInter1 and dsid=$uid and event='SERVICE-END')"
        existsEv <- sqlEv.query[Boolean].unique.transact(res.xa)
        _        <- expect(existsEv).failFast

        sqlDs = sql"select exists (select id from data_subjects where id=$uid)"
        userInDb <- sqlDs.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast

      } yield success
  }

}
