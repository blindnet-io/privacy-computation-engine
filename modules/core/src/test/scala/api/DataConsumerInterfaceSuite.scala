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
import doobie.postgres.circe.jsonb.implicits.*
import io.blindnet.pce.priv.*
import io.blindnet.pce.services.*
import io.blindnet.pce.util.*
import io.blindnet.pce.util.extension.*
import io.blindnet.pce.priv.terms.{ LegalBaseTerms, EventTerms, RetentionPolicyTerms }
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
import io.blindnet.pce.model.*
import priv.DataSubject
import priv.privacyrequest.{ Demand, PrivacyRequest, * }
import priv.LegalBase
import SharedResources.*
import testutil.*
import httputil.*
import scala.concurrent.duration.*
import io.blindnet.pce.priv.terms.{ Status as PrivStatus, * }
import io.blindnet.pce.priv.terms.Action.*
import io.blindnet.pce.api.endpoints.messages.*
import io.blindnet.pce.api.endpoints.messages.consumerinterface.*
import com.github.dockerjava.api.model.EventType

class DataConsumerInterface(global: GlobalRead) extends IOSuite {

  val appId = uuid
  val dsId1 = uuid.toString
  val dsId2 = uuid.toString

  val (r1, r2, r3, r4, r5, r6) = (uuid, uuid, uuid, uuid, uuid, uuid)

  val (d11, d12, d21, d22, d23, d31, d32, d33, d41, d42, d51, d61) =
    (uuid, uuid, uuid, uuid, uuid, uuid, uuid, uuid, uuid, uuid, uuid, uuid)

  val t = now - 10

  val consent1       = uuid
  val consent2       = uuid
  val contract1      = uuid
  val necessary1     = uuid
  val legitInterest1 = uuid

  type Res = Resources
  def sharedResource: Resource[IO, Res] =
    for {
      res <- sharedResourceOrFallback(global)
      _   <- Resource.eval(sql"""insert into apps values ($appId)""".update.run.transact(res.xa))
      _   <- Resource.eval(sql"""
      insert into data_subjects values
      ($dsId1, $appId, 'id'),
      ($dsId2, $appId, 'id')
      """.update.run.transact(res.xa))

      // ----------------------------------------
      // request 1
      _   <- Resource.eval(sql"""
      insert into privacy_requests values
      ($r1, $appId, $dsId1, ${List(dsId1)}, $t, 'ORGANIZATION')
      """.update.run.transact(res.xa))

      // demands
      _   <- Resource.eval(sql"""
      insert into demands values
      ($d11, $r1, 'ACCESS', 'msg11', 'lang11')
      """.update.run.transact(res.xa))
      _   <- Resource.eval(sql"""
      insert into demands values
      ($d12, $r1, 'TRANSPARENCY', 'msg12', 'lang12')
      """.update.run.transact(res.xa))

      // recommendations
      _   <- Resource.eval(sql"""
      insert into demand_recommendations values
      ($uuid, $d11, 'GRANTED', null, ${List("BIOMETRIC", "DEVICE")},
        ${t - 10}, ${t - 5}, 'USER', 'ORGANIZATION')
      """.update.run.transact(res.xa))
      _   <- Resource.eval(sql"""
      insert into demand_recommendations values
      ($uuid, $d12, 'GRANTED', null, ${List("BIOMETRIC", "DEVICE")},
        ${t - 10}, ${t - 5}, 'USER', 'ORGANIZATION')
      """.update.run.transact(res.xa))
      // ----------------------------------------

      // ----------------------------------------
      // request 2
      _   <- Resource.eval(sql"""
      insert into privacy_requests values
      ($r2, $appId, $dsId2, ${List(dsId2)}, $t, 'ORGANIZATION')
      """.update.run.transact(res.xa))

      // demands
      _   <- Resource.eval(sql"""
      insert into demands values
      ($d21, $r2, 'DELETE', 'msg21', 'lang21')
      """.update.run.transact(res.xa))
      _   <- Resource.eval(sql"""
      insert into demands values
      ($d22, $r2, 'TRANSPARENCY.DPO', 'msg22', 'lang22')
      """.update.run.transact(res.xa))
      _   <- Resource.eval(sql"""
      insert into demands values
      ($d23, $r2, 'TRANSPARENCY.DPO', 'msg23', 'lang23')
      """.update.run.transact(res.xa))

      // recommendations
      _   <- Resource.eval(sql"""
      insert into demand_recommendations values
      ($uuid, $d21, 'DENIED', null, ${List("BIOMETRIC", "DEVICE")},
        ${t - 10}, ${t - 5}, 'USER', 'ORGANIZATION')
      """.update.run.transact(res.xa))
      _   <- Resource.eval(sql"""
      insert into demand_recommendations values
      ($uuid, $d22, 'DENIED', null, ${List("BIOMETRIC", "DEVICE")},
        ${t - 10}, ${t - 5}, 'USER', 'ORGANIZATION')
      """.update.run.transact(res.xa))
      // ----------------------------------------

      // ----------------------------------------
      // request 3
      _   <- Resource.eval(sql"""
      insert into privacy_requests values
      ($r3, $appId, $dsId1, ${List(dsId1)}, $t, 'ORGANIZATION')
      """.update.run.transact(res.xa))

      // demands
      _   <- Resource.eval(sql"""
      insert into demands values
      ($d31, $r3, 'RESTRICT', 'msg31', 'lang31')
      """.update.run.transact(res.xa))
      _   <- Resource.eval(sql"""
      insert into demands values
      ($d32, $r3, 'DELETE', 'msg32', 'lang32')
      """.update.run.transact(res.xa))

      // response
      d32r = uuid
      _ <- Resource.eval(sql"""
      insert into privacy_responses values
      ($d32r, $d32, null, 'DELETE', null)
      """.update.run.transact(res.xa))
      _ <- Resource.eval(sql"""
      insert into privacy_response_events values
      ($uuid, $d32r, ${t + 1}, 'UNDER-REVIEW', null, null, null, null)
      """.update.run.transact(res.xa))
      _ <- Resource.eval(sql"""
      insert into privacy_response_events values
      ($uuid, $d32r, ${t + 2}, 'DENIED', 'OTHER-MOTIVE', 'denied', 'en', null)
      """.update.run.transact(res.xa))
      // ----------------------------------------

      // ----------------------------------------
      // request 4
      _ <- Resource.eval(sql"""
      insert into privacy_requests values
      ($r4, $appId, $dsId2, ${List(dsId2)}, $t, 'ORGANIZATION')
      """.update.run.transact(res.xa))

      // demands
      _ <- Resource.eval(sql"""
      insert into demands values
      ($d41, $r4, 'REVOKE-CONSENT', 'msg41', 'lang41')
      """.update.run.transact(res.xa))
      _ <- Resource.eval(sql"""
      insert into demands values
      ($d42, $r4, 'RESTRICT', 'msg42', 'lang42')
      """.update.run.transact(res.xa))
      // ----------------------------------------

      // ----------------------------------------
      // request 5
      _ <- Resource.eval(sql"""
      insert into privacy_requests values
      ($r5, $appId, null, ${List.empty[String]}, $t, 'ORGANIZATION')
      """.update.run.transact(res.xa))

      // demands
      _ <- Resource.eval(sql"""
      insert into demands values
      ($d51, $r5, 'TRANSPARENCY.DPO', 'msg51', 'lang51')
      """.update.run.transact(res.xa))

      // response
      d51r = uuid
      _ <- Resource.eval(sql"""
      insert into privacy_responses values
      ($d51r, $d51, null, 'TRANSPARENCY.DPO', null)
      """.update.run.transact(res.xa))
      _ <- Resource.eval(sql"""
      insert into privacy_response_events values
      ($uuid, $d51r, ${t + 1}, 'UNDER-REVIEW', null, null, null, null)
      """.update.run.transact(res.xa))
      _ <- Resource.eval(sql"""
      insert into privacy_response_events values
      ($uuid, $d51r, ${t + 2}, 'GRANTED', null, null, null, '"dpo"')
      """.update.run.transact(res.xa))
      // ----------------------------------------

      // ----------------------------------------
      // different app
      // request 6
      appId2 = uuid
      _ <- Resource.eval(sql"""insert into apps values ($appId2)""".update.run.transact(res.xa))
      dsApp2 = uuid
      _ <- Resource.eval(sql"""
      insert into data_subjects values
      ($dsApp2, $appId2, 'id')
      """.update.run.transact(res.xa))

      _ <- Resource.eval(sql"""
      insert into privacy_requests values
      ($r6, $appId2, $dsApp2, ${List(dsApp2)}, $t, 'ORGANIZATION')
      """.update.run.transact(res.xa))

      // demands
      _ <- Resource.eval(sql"""
      insert into demands values
      ($d61, $r6, 'REVOKE-CONSENT', 'msg61', 'lang61')
      """.update.run.transact(res.xa))
      // ----------------------------------------

      // pending demands
      _ <- Resource.eval(sql"""
      insert into pending_demands_to_review values
      ($d11), ($d12), ($d21), ($d22), ($d31), ($d61)
      """.update.run.transact(res.xa))

      // legal bases
      _ <- Resource.eval(sql"""
      insert into legal_bases values
      ($consent1, $appId, 'CONSENT', 'test consent 1', '', true),
      ($consent2, $appId, 'CONSENT', 'test consent 2', '', true),
      ($necessary1, $appId, 'NECESSARY', 'test necessary 1', '', true),
      ($contract1, $appId, 'CONTRACT', 'test contract 1', '', true),
      ($legitInterest1, $appId, 'LEGITIMATE-INTEREST', 'test legitimate interest 1', '', true)
      """.update.run.transact(res.xa))

      // legal base events
      _ <- Resource.eval(sql"""
      insert into legal_base_events values
      ($uuid, $necessary1, $dsId1, $appId, 'RELATIONSHIP-START', ${t - 10}),
      ($uuid, $contract1, $dsId1, $appId, 'SERVICE-START', ${t - 9}),
      ($uuid, $contract1, $dsId1, $appId, 'SERVICE-END', ${t - 8}),
      ($uuid, $legitInterest1, $dsId1, $appId, 'RELATIONSHIP-END', ${t - 7})
      """.update.run.transact(res.xa))

      _ <- Resource.eval(sql"""
      insert into consent_given_events values
      ($uuid, $consent1, $dsId1, $appId, ${t - 10}),
      ($uuid, $consent2, $dsId1, $appId, ${t - 8})
      """.update.run.transact(res.xa))

      _ <- Resource.eval(sql"""
      insert into consent_revoked_events values
      ($uuid, $consent1, $dsId1, $appId, ${t - 7})
      """.update.run.transact(res.xa))

    } yield res

  test("get pending demands") {
    res =>
      for {
        resp    <- res.server.run(get("consumer-interface/pending-requests", Some(appToken(appId))))
        pending <- resp.to[List[PendingDemandPayload]]
        _       <- expect
          .all(
            pending.length == 5,
            pending.exists(_.id == d11),
            pending.exists(_.id == d12),
            pending.exists(_.id == d21),
            pending.exists(_.id == d22),
            pending.exists(_.id == d31)
          )
          .failFast
      } yield success
  }

  test("fail getting non-existent pending demand") {
    res =>
      for {
        resp <- res.server.run(
          get(s"consumer-interface/pending-requests/$uuid", Some(appToken(appId)))
        )
        _    <- expect(resp.status == Status.NotFound).failFast
      } yield success
  }

  test("get details of a pending demand") {
    res =>
      for {
        resp <- res.server.run(
          get(s"consumer-interface/pending-requests/${d11}", Some(appToken(appId)))
        )
        d    <- resp.to[PendingDemandDetailsPayload]
        r = d.recommendation.get
        _ <- expect
          .all(
            d.id == d11,
            d.action == Access,
            d.dataSubject.contains(DataSubjectPayload(dsId1, None)),
            r.dId == d11,
            r.status.contains(PrivStatus.Granted),
            r.dataCategories == Set(
              DataCategory("BIOMETRIC"),
              DataCategory("DEVICE")
            ),
            r.provenance.contains(ProvenanceTerms.User),
            r.target.contains(Target.Organization)
          )
          .failFast
      } yield success
  }

  test("fail approving demand if not pending") {
    res =>
      val req = json"""{ "id": $d23 }"""
      for {
        resp <- res.server.run(
          post(s"consumer-interface/pending-requests/approve", req, Some(appToken(appId)))
        )
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("fail approving demand if no recommendation is set") {
    res =>
      val req = json"""{ "id": $d31 }"""
      for {
        resp <- res.server.run(
          post(s"consumer-interface/pending-requests/approve", req, Some(appToken(appId)))
        )
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("approve demand") {
    res =>
      val req = json"""
      {
        "id": $d11,
        "msg": "resp_msg_11",
        "lang": "resp_lang_11"
      }
      """
      for {
        resp   <- res.server.run(
          post(s"consumer-interface/pending-requests/approve", req, Some(appToken(appId)))
        )
        status <- sql"""select status from demand_recommendations where did=$d11"""
          .query[PrivStatus]
          .unique
          .transact(res.xa)
        exists <- res.repos.demandsToReview.exists(appId, d11)
        data   <- sql"""select data from commands_create_response where did=$d11"""
          .query[Json]
          .unique
          .transact(res.xa)
        expData = json"""{ "msg": "resp_msg_11", "lang": "resp_lang_11" }"""
        _ <- expect
          .all(
            status == PrivStatus.Granted,
            !exists,
            data == expData
          )
          .failFast
      } yield success
  }

  test("fail denying demand if not pending") {
    res =>
      val req = json"""{ "id": $d23, "motive": ${Motive.ValidReasons.encode} }"""
      for {
        resp <- res.server.run(
          post(s"consumer-interface/pending-requests/deny", req, Some(appToken(appId)))
        )
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("fail denying demand if no recommendation is set") {
    res =>
      val req = json"""{ "id": $d31, "motive": ${Motive.ValidReasons.encode} }"""
      for {
        resp <- res.server.run(
          post(s"consumer-interface/pending-requests/deny", req, Some(appToken(appId)))
        )
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("deny demand") {
    res =>
      val req = json"""
      {
        "id": $d12,
        "motive": ${Motive.ValidReasons.encode},
        "msg": "resp_msg_12",
        "lang": "resp_lang_12"
      }
      """
      for {
        resp   <- res.server.run(
          post(s"consumer-interface/pending-requests/deny", req, Some(appToken(appId)))
        )
        status <- sql"""select status from demand_recommendations where did=$d12"""
          .query[PrivStatus]
          .unique
          .transact(res.xa)
        exists <- res.repos.demandsToReview.exists(appId, d12)
        data   <- sql"""select data from commands_create_response where did=$d12"""
          .query[Json]
          .unique
          .transact(res.xa)
        expData = json"""{ "msg": "resp_msg_12", "lang": "resp_lang_12" }"""
        _ <- expect
          .all(
            status == PrivStatus.Denied,
            !exists,
            data == expData
          )
          .failFast
      } yield success
  }

  test("fail updating demand recommendation if not pending") {
    res =>
      val req = json"""
      {
        "demand_id": $d23,
        "recommendation": {
          "status": ${PrivStatus.Denied.encode},
          "motive": ${Motive.UserUnknown.encode}
        }
      }
      """
      for {
        resp <- res.server.run(
          post(s"consumer-interface/pending-requests/recommendation", req, Some(appToken(appId)))
        )
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("fail updating demand recommendation if status DENIED and no motive is set") {
    res =>
      val req = json"""
      {
        "demand_id": $d22,
        "recommendation": {
          "status": ${PrivStatus.Denied.encode}
        }
      }
      """
      for {
        resp <- res.server.run(
          post(s"consumer-interface/pending-requests/recommendation", req, Some(appToken(appId)))
        )
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("update demand recommendation") {
    res =>
      val req = json"""
      {
        "demand_id": $d21,
        "recommendation": {
          "status": ${PrivStatus.Denied.encode},
          "motive": ${Motive.UserUnknown.encode},
          "data_categories": ["AFFILIATION"],
          "provenance": "USER",
          "target": "SYSTEM"
        }
      }
      """
      for {
        resp <- res.server.run(
          post(s"consumer-interface/pending-requests/recommendation", req, Some(appToken(appId)))
        )
        r    <- res.repos.privacyRequest.getRecommendation(d21).map(_.get)

        _ <- expect
          .all(
            r.status.contains(PrivStatus.Denied),
            r.motive.contains(Motive.UserUnknown),
            r.dataCategories == Set(DataCategory("AFFILIATION")),
            r.provenance.contains(ProvenanceTerms.User),
            r.target.contains(Target.System)
          )
          .failFast
      } yield success
  }

  test("get completed demands") {
    res =>
      for {
        resp <- res.server.run(get("consumer-interface/completed-requests", Some(appToken(appId))))
        completed <- resp.to[List[CompletedDemandPayload]]
        _         <- expect
          .all(
            completed.length == 2,
            completed.exists(
              c =>
                c.id == d51 && c.action == Action.TDPO && c.dataSubject.isEmpty && c.status == PrivStatus.Granted
            ),
            completed.exists(
              c =>
                c.id == d32 && c.action == Action.Delete &&
                  c.dataSubject.contains(DataSubjectPayload(dsId1)) && c.status == PrivStatus.Denied
            )
          )
          .failFast
      } yield success
  }

  test("fail getting completed demand details for non-existent demand") {
    res =>
      for {
        resp <- res.server.run(
          get(s"consumer-interface/completed-requests/$uuid", Some(appToken(appId)))
        )
        _    <- expect(resp.status == Status.NotFound).failFast
      } yield success
  }

  test("return empty response details for non-completed demand") {
    res =>
      for {
        resp <- res.server.run(
          get(s"consumer-interface/completed-requests/$d11", Some(appToken(appId)))
        )
        c    <- resp.to[List[CompletedDemandInfoPayload]]
        _    <- expect(c.isEmpty).failFast
      } yield success
  }

  test("get completed denied delete demand details") {
    res =>
      for {
        resp <- res.server.run(
          get(s"consumer-interface/completed-requests/$d32", Some(appToken(appId)))
        )
        c    <- resp.to[List[CompletedDemandInfoPayload]].map(_.head)
        _    <- expect
          .all(
            c.demandId == d32,
            c.action == Action.Delete,
            c.status == PrivStatus.Denied,
            c.motive.contains(Motive.OtherMotive),
            c.answer.isEmpty,
            c.requestMessage.contains("msg32"),
            c.requestLang.contains("lang32"),
            c.responseMessage.contains("denied"),
            c.responseLang.contains("en")
          )
          .failFast
      } yield success
  }

  test("get completed granted transparency demand details") {
    res =>
      for {
        resp <- res.server.run(
          get(s"consumer-interface/completed-requests/$d51", Some(appToken(appId)))
        )
        c    <- resp.to[List[CompletedDemandInfoPayload]].map(_.head)
        _    <- expect
          .all(
            c.demandId == d51,
            c.action == Action.TDPO,
            c.status == PrivStatus.Granted,
            c.motive.isEmpty,
            c.answer.contains("\"dpo\""),
            c.requestMessage.contains("msg51"),
            c.requestLang.contains("lang51"),
            c.responseMessage.isEmpty,
            c.responseLang.isEmpty
          )
          .failFast
      } yield success
  }

  test("get timeline") {
    res =>
      for {
        resp <- res.server.run(get(s"consumer-interface/timeline/$dsId1", Some(appToken(appId))))
        t    <- resp.to[TimelineEventsPayload]
        _    <- expect
          .all(
            t.requests.length == 2,
            t.requests.exists(
              r =>
                r.id == r1 && r.target == Target.Organization && r.demands.toSet == Set(
                  PrivacyRequestEventDemand(d11, Action.Access),
                  PrivacyRequestEventDemand(d12, Action.Transparency)
                )
            ),
            t.requests.exists(
              r =>
                r.id == r3 && r.target == Target.Organization && r.demands.toSet == Set(
                  PrivacyRequestEventDemand(d31, Action.Restrict),
                  PrivacyRequestEventDemand(d32, Action.Delete)
                )
            ),
            t.responses.length == 1,
            t.responses.exists(
              r =>
                r.demandId == d32 &&
                  r.action == Action.Delete &&
                  r.includes == List.empty &&
                  r.status == PrivStatus.Denied
            ),
            t.givenConsents.length == 2,
            t.givenConsents.exists(gc => gc.id == consent1 && gc.name.contains("test consent 1")),
            t.givenConsents.exists(gc => gc.id == consent2 && gc.name.contains("test consent 2")),
            t.revokedConsents.length == 1,
            t.givenConsents.exists(gc => gc.id == consent1 && gc.name.contains("test consent 1")),
            t.legalBases.length == 4,
            t.legalBases.exists(
              lb =>
                lb.id == necessary1 && lb.event == EventTerms.RelationshipStart && lb.`type` == LegalBaseTerms.Necessary && lb.name
                  .contains("test necessary 1")
            ),
            t.legalBases.exists(
              lb =>
                lb.id == contract1 && lb.event == EventTerms.ServiceStart && lb.`type` == LegalBaseTerms.Contract && lb.name
                  .contains("test contract 1")
            ),
            t.legalBases.exists(
              lb =>
                lb.id == contract1 && lb.event == EventTerms.ServiceEnd && lb.`type` == LegalBaseTerms.Contract && lb.name
                  .contains("test contract 1")
            ),
            t.legalBases.exists(
              lb =>
                lb.id == legitInterest1 && lb.event == EventTerms.RelationshipEnd && lb.`type` == LegalBaseTerms.LegitimateInterest && lb.name
                  .contains("test legitimate interest 1")
            )
          )
          .failFast
      } yield success
  }

}
