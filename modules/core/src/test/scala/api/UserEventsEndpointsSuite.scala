package io.blindnet.pce
package api

import java.time.Instant
import java.util.UUID

import scala.concurrent.duration.*
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
import SharedResources.*
import testutil.*
import httputil.*
import dbutil.*
import io.blindnet.pce.priv.terms.LegalBaseTerms

class UserEventsEndpointsSuite(global: GlobalRead) extends IOSuite {

  val appId = uuid

  val ds = DataSubject(uuid.toString, appId)

  val consent1  = uuid
  val consent2  = uuid
  val consent3  = uuid
  val contract1 = uuid
  val legInter1 = uuid

  type Res = Resources
  def sharedResource: Resource[IO, Resources] =
    for {
      res <- global.getOrFailR[Resources]()

      _ <- Resource.eval {
        for {
          _ <- createApp(appId, res.xa)
          _ <- createDs(ds.id, appId, res.xa)

          _ <- sql"""
          insert into data_categories (id, term, selector, appid, active) values
          ($uuid, 'OTHER-DATA.PROOF', true, $appId, true)
          """.update.run.transact(res.xa)

          _ <- sql"""
          insert into provenances (select gen_random_uuid(), $appId, id, 'USER', 'demo' from data_categories)
          """.update.run.transact(res.xa)

          _ <- sql"""insert into retention_policies (
          select gen_random_uuid(), $appId, id, 'NO-LONGER-THAN', '10', 'RELATIONSHIP-END' from data_categories)
          """.update.run.transact(res.xa)

          _ <- sql"""insert into "scope" (
          select gen_random_uuid() as id, dc.id as dcid, pc.id as pcid, pp.id as ppid
          from data_categories dc, processing_categories pc, processing_purposes pp
          where dc.selector = true)
          """.update.run.transact(res.xa)

          _ <- createLegalBase(consent1, appId, LegalBaseTerms.Consent, "Prizes consent", res.xa)
          _ <- createLegalBase(consent2, appId, LegalBaseTerms.Consent, "test consent 1", res.xa)
          _ <- createLegalBase(consent3, appId, LegalBaseTerms.Consent, "test consent 2", res.xa)
          _ <- createLegalBase(contract1, appId, LegalBaseTerms.Contract, "test contract 1", res.xa)
          _ <- createLegalBase(
            legInter1,
            appId,
            LegalBaseTerms.LegitimateInterest,
            "test legitimate interest 1",
            res.xa
          )

          _ <- sql"""
          insert into legal_bases_scope
          values ($consent1, (
            select s.id from scope s
            join data_categories dc on dc.id = s.dcid
            join processing_categories pc on pc.id = s.pcid
            join processing_purposes pp on pp.id = s.ppid
            where dc.term = 'CONTACT.EMAIL' and pc.term='*' and pp.term = '*')
          )
          """.update.run.transact(res.xa)

          _ <- sql"""
          insert into legal_bases_scope
          values ($consent1, (
            select s.id from scope s
            join data_categories dc on dc.id = s.dcid
            join processing_categories pc on pc.id = s.pcid
            join processing_purposes pp on pp.id = s.ppid
            where dc.term = 'NAME' and pc.term='*' and pp.term = '*')
          )
          """.update.run.transact(res.xa)

          _ <- sql"""
          insert into legal_bases_scope
          values ($consent1, (
            select s.id from scope s
            join data_categories dc on dc.id = s.dcid
            join processing_categories pc on pc.id = s.pcid
            join processing_purposes pp on pp.id = s.ppid
            where dc.term = 'UID.ID' and pc.term='*' and pp.term = '*')
          )
          """.update.run.transact(res.xa)

          _ <- sql"""
          insert into legal_bases_scope
          values ($consent1, (
            select s.id from scope s
            join data_categories dc on dc.id = s.dcid
            join processing_categories pc on pc.id = s.pcid
            join processing_purposes pp on pp.id = s.ppid
            where dc.term = 'OTHER-DATA.PROOF' and pc.term='*' and pp.term = '*')
          )
          """.update.run.transact(res.xa)

          _ <- sql"""
          insert into consent_given_events (id, lbid, dsid, appid, date) values
          ($uuid, $consent1, ${ds.id}, $appId,  LOCALTIMESTAMP - INTERVAL '185 DAY');
          """.update.run.transact(res.xa)
        } yield ()
      }
    } yield res

  test("fail proactively giving consent for large scope") {
    res =>
      val req = json"""
      {
          "scope": [
            { "dc": "*", "pc": "*", "pp": "*" }
          ]
      }
      """
      for {
        response <- res.server.run(
          post("user-events/consent/proactive", req, userToken(appId, ds.id))
        )
        _        <- expect(response.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  def waitUntilLbInserted(xa: Transactor[IO], id: UUID, n: Int = 0): IO[Unit] =
    if n == 10 then failure("Could not find legal base in db")
    for {
      inserted <- sql"""select exists (select lbid from legal_bases_scope where lbid=$id)"""
        .query[Boolean]
        .unique
        .transact(xa)
      _        <- (IO.sleep(1.seconds) *> waitUntilLbInserted(xa, id, n + 1)).unlessA(inserted)
    } yield ()

  test("create and record proactively given consent for known user") {
    res =>
      val req = json"""
      {
          "scope": [
            { "dc": "CONTACT", "pc": "*", "pp": "ADVERTISING" },
            { "dc": "NAME", "pc": "GENERATING", "pp": "RESEARCH" }
          ]
      }
      """
      for {
        add       <- res.server.run(
          post("user-events/consent/proactive", req, userToken(appId, ds.id))
        )
        consentId <- add.as[String].map(_.uuid)

        q1 = sql"select exists (select id from legal_bases where id=$consentId)"
        lbInDb <- q1.query[Boolean].unique.transact(res.xa)
        _      <- expect(lbInDb).failFast

        _ <- waitUntilLbInserted(res.xa, consentId)
        q2 = sql"""select count(*) from legal_bases_scope where lbid = $consentId"""
        rows <- q2.query[Int].unique.transact(res.xa)
        _    <- expect(rows == 31).failFast

        q3 =
          sql"select exists (select lbid from consent_given_events where lbid=$consentId and dsid=${ds.id})"
        existsEv <- q3.query[Boolean].unique.transact(res.xa)
        _        <- expect(existsEv).failFast

      } yield success
  }

  test("create and record proactively given consent for unknown user") {
    res =>
      val uid = uuid.toString
      val req = json"""
      {
          "scope": [
            { "dc": "CONTACT", "pc": "*", "pp": "RESEARCH" }
          ]
      }
      """
      for {
        add       <- res.server.run(
          post("user-events/consent/proactive", req, userToken(appId, uid))
        )
        consentId <- add.as[String].map(_.uuid)

        q1 = sql"select exists (select id from legal_bases where id=$consentId)"
        lbInDb <- q1.query[Boolean].unique.transact(res.xa)
        _      <- expect(lbInDb).failFast

        _ <- waitUntilLbInserted(res.xa, consentId)
        q2 = sql"""select count(*) from legal_bases_scope where lbid = $consentId"""
        rows <- q2.query[Int].unique.transact(res.xa)
        _    <- expect(rows == 30).failFast

        sqlDs = sql"select exists (select id from data_subjects where id=$uid)"
        userInDb <- sqlDs.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast

        q3 =
          sql"select exists (select lbid from consent_given_events where lbid=$consentId and dsid=$uid)"
        existsEv <- q3.query[Boolean].unique.transact(res.xa)
        _        <- expect(existsEv).failFast

      } yield success
  }

  test("record known proactively given consent for user") {
    res =>
      val uid1 = uuid.toString
      val uid2 = uuid.toString
      val req  = json"""
      {
          "scope": [
            { "dc": "CONTACT", "pc": "*", "pp": "*" }
          ]
      }
      """
      for {
        add        <- res.server.run(
          post("user-events/consent/proactive", req, userToken(appId, uid1))
        )
        consentId1 <- add.as[String].map(_.uuid)

        _ <- waitUntilLbInserted(res.xa, consentId1)

        add        <- res.server.run(
          post("user-events/consent/proactive", req, userToken(appId, uid2))
        )
        consentId2 <- add.as[String].map(_.uuid)
        _          <- expect(consentId1 == consentId2).failFast

        q3 =
          sql"select exists (select lbid from consent_given_events where lbid=$consentId2 and dsid=$uid2)"
        existsEv <- q3.query[Boolean].unique.transact(res.xa)
        _        <- expect(existsEv).failFast

      } yield success
  }

  test("fail recording given consent for unknown consent id") {
    res =>
      val req = json"""{ "consent_id": $uuid }"""
      res.server
        .run(post("user-events/consent", req, userToken(appId, ds.id)))
        .map(response => expect(response.status == Status.NotFound))
  }

  test("record given consent for known user") {
    res =>
      val req = json"""{ "consent_id": $consent1 }"""
      for {
        response <- res.server.run(post("user-events/consent", req, userToken(appId, ds.id)))
        _        <- expect(response.status == Status.Ok).failFast
        sql =
          sql"select exists (select lbid from consent_given_events where lbid=$consent1 and dsid=${ds.id})"
        userInDb <- sql.query[Boolean].unique.transact(res.xa)
        _        <- expect(userInDb).failFast
      } yield success
  }

  test("record given consent for unknown user") {
    res =>
      val uid = uuid.toString
      val req = json"""{ "consent_id": $consent1 }"""
      for {
        response <- res.server.run(post("user-events/consent", req, userToken(appId, uid)))
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
        .run(post("user-events/consent/store", req, appToken(appId)))
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
        response <- res.server.run(post("user-events/consent/store", req, appToken(appId)))
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
        response <- res.server.run(post("user-events/consent/store", req, appToken(appId)))
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
        .run(post("user-events/contract/start", req, appToken(appId)))
        .map(response => expect(response.status == Status.NotFound))
  }

  test("store start of contract known user") {
    res =>
      val req =
        json"""{ "data_subject": {"id": ${ds.id}}, "contract_id": $contract1, "date": $now }"""
      for {
        response <- res.server.run(post("user-events/contract/start", req, appToken(appId)))
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
        response <- res.server.run(post("user-events/contract/start", req, appToken(appId)))
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
        .run(post("user-events/contract/end", req, appToken(appId)))
        .map(response => expect(response.status == Status.NotFound))
  }

  test("store end of contract known user") {
    res =>
      val req =
        json"""{ "data_subject": {"id": ${ds.id}}, "contract_id": $contract1, "date": $now }"""
      for {
        response <- res.server.run(post("user-events/contract/end", req, appToken(appId)))
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
        response <- res.server.run(post("user-events/contract/end", req, appToken(appId)))
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
        .run(post("user-events/legitimate-interest/start", req, appToken(appId)))
        .map(response => expect(response.status == Status.NotFound))
  }

  test("store start of legitimate interest known user") {
    res =>
      val req =
        json"""{ "data_subject": {"id": ${ds.id}}, "legitimate_interest_id": $legInter1, "date": $now }"""
      for {
        response <- res.server.run(
          post("user-events/legitimate-interest/start", req, appToken(appId))
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
          post("user-events/legitimate-interest/start", req, appToken(appId))
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
        .run(post("user-events/legitimate-interest/end", req, appToken(appId)))
        .map(response => expect(response.status == Status.NotFound))
  }

  test("store end of legitimate interest known user") {
    res =>
      val req =
        json"""{ "data_subject": {"id": ${ds.id}}, "legitimate_interest_id": $legInter1, "date": $now }"""
      for {
        response <- res.server.run(
          post("user-events/legitimate-interest/end", req, appToken(appId))
        )
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
        response <- res.server.run(
          post("user-events/legitimate-interest/end", req, appToken(appId))
        )
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
