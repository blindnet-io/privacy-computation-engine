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
import io.blindnet.pce.api.endpoints.messages.configuration.*
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
import io.blindnet.pce.priv.terms.ProvenanceTerms
import io.blindnet.pce.priv.terms.DataCategory

class ConfigurationEndpointsSuite(global: GlobalRead) extends IOSuite {

  val appId = uuid

  case class LocalResources(
      legalBasesAdded: CountDownLatch[IO],
      legalBaseIds: Ref[IO, List[UUID]]
  )

  type Res = (Resources, LocalResources)
  def sharedResource: Resource[IO, Res] =
    for {
      res <- sharedResourceOrFallback(global)
      _   <- Resource.eval {
        for {
          _ <- dbutil.createApp(appId, res.xa)
          _ <- dbutil.createRegulations(res.xa)

          _ <- sql"""
          insert into data_categories (id, term, selector, appid, active) values
          ($uuid, 'OTHER-DATA.PROOF', true, $appId, true)
          """.update.run.transact(res.xa)

          _ <- sql"""insert into "scope" (
          select gen_random_uuid() as id, dc.id as dcid, pc.id as pcid, pp.id as ppid
          from data_categories dc, processing_categories pc, processing_purposes pp
          where dc.selector = true and dc.appid = $appId)
          """.update.run.transact(res.xa)

          _ <- sql"""
          insert into provenances (
            select gen_random_uuid(), $appId, id, 'USER', 'demo' from data_categories
            where term = 'DEVICE'
          )
          """.update.run.transact(res.xa)

          _ <- sql"""insert into retention_policies (
            select gen_random_uuid(), $appId, id, 'NO-LONGER-THAN', '10', 'RELATIONSHIP-END' from data_categories
            where term = 'DEVICE'
          )
          """.update.run.transact(res.xa)
        } yield ()
      }

      lbsAdded <- Resource.eval(CountDownLatch[IO](4))
      lbIds    <- Resource.eval(Ref.of(List.empty[UUID]))
      localRes = LocalResources(lbsAdded, lbIds)
    } yield (res, localRes)

  test("set and get general information") {
    res =>
      val req = json"""
      {
          "countries": ["France"],
          "organization": "new-test",
          "dpo": "dpo@fakemail.com",
          "data_consumer_categories": ["dc cat 1", "dc cat 2", "dc cat 66"],
          "data_security_info": ""
      }
      """
      for {
        _    <- res._1.server.run(put("configure/general-info", req, appToken(appId)))
        resp <- res._1.server.run(get("configure/general-info", appToken(appId)))
        gi   <- resp.to[GeneralInformation]
        _    <- expect
          .all(
            gi.countries.sorted == List("France"),
            gi.organization == "new-test",
            gi.dpo == "dpo@fakemail.com",
            gi.dataConsumerCategories.sorted == List("dc cat 1", "dc cat 2", "dc cat 66"),
            gi.privacyPolicyLink == None,
            gi.dataSecurityInfo == Some("")
          )
          .failFast
      } yield success
  }

  test("set and get demand resolution strategy") {
    res =>
      val auto = DemandResolution.Automatic
      val man  = DemandResolution.Manual
      val req  = json"""
      {
          "transparency": "manual",
          "access": "auto",
          "delete": "manual",
          "revoke_consent": "auto",
          "object": "manual",
          "restrict": "auto"
      }
      """
      for {
        _    <- res._1.server.run(
          put("configure/demand-resolution-strategy", req, appToken(appId))
        )
        resp <- res._1.server.run(
          get("configure/demand-resolution-strategy", appToken(appId))
        )
        s    <- resp.to[DemandResolutionStrategy]
        _    <- expect
          .all(
            s.transparency == man,
            s.access == auto,
            s.delete == man,
            s.revokeConsent == auto,
            s.objectScope == man,
            s.restrictScope == auto
          )
          .failFast
      } yield success
  }

  test("add selectors") {
    res =>
      val req = json"""
      [
        { "name": "selector_1", "data_category": "AFFILIATION" },
        { "name": "selector_1", "data_category": "AFFILIATION" },
        { "name": "selector_2", "data_category": "LOCATION" },
        { "name": "selector_3", "data_category": "CONTACT.PHONE" }
      ]
      """
      for {
        _ <- res._1.server.run(put("configure/selectors", req, appToken(appId)))

        dcs <- sql"""select term from data_categories where selector=true"""
          .query[String]
          .to[List]
          .transact(res._1.xa)
        _   <- expect
          .all(
            dcs.contains("AFFILIATION.selector_1"),
            dcs.contains("LOCATION.selector_2"),
            dcs.contains("CONTACT.PHONE.selector_3")
          )
          .failFast

        rows <-
          sql"""select count(*) from scope where dcid = (select id from data_categories where term='LOCATION')"""
            .query[Int]
            .unique
            .transact(res._1.xa)
        _    <- expect(rows == 209).failFast
      } yield success
  }

  test("added selectors copy parents provenances and retention policies") {
    res =>
      val req = json"""
      [
        { "name": "selector_copied_1", "data_category": "DEVICE" }
      ]
      """
      for {
        _ <- res._1.server.run(put("configure/selectors", req, appToken(appId)))

        provenances <- res._1.repos.provenance.get(appId, DataCategory("DEVICE.selector_copied_1"))
        _           <- expect
          .all(
            provenances.head.provenance == ProvenanceTerms.User,
            provenances.head.system == "demo"
          )
          .failFast

        retPolicies <- res._1.repos.retentionPolicy
          .get(appId, DataCategory("DEVICE.selector_copied_1"))
        _           <- expect
          .all(
            retPolicies.head.policyType == RetentionPolicyTerms.NoLongerThan,
            retPolicies.head.duration == "10",
            retPolicies.head.after == EventTerms.RelationshipEnd
          )
          .failFast

      } yield success
  }

  test("fail adding selectors for * data category") {
    res =>
      val req = json"""
      [
        { "name": "selector_1", "data_category": "*" }
      ]
      """
      for {
        resp <- res._1.server.run(put("configure/selectors", req, appToken(appId)))
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("fail adding selectors if nothing provided") {
    res =>
      val req = json"""[]"""
      for {
        resp <- res._1.server.run(put("configure/selectors", req, appToken(appId)))
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("fail adding selectors for non existing data category") {
    res =>
      val req = json"""
      [
        { "name": "selector_1", "data_category": "DEMOGRAPHIC" },
        { "name": "selector_1", "data_category": "TEST" }
      ]
      """
      for {
        resp <- res._1.server.run(put("configure/selectors", req, appToken(appId)))
        _    <- expect(resp.status == Status.BadRequest).failFast
      } yield success
  }

  test("fail adding selectors for non existing data subcategory") {
    res =>
      val req = json"""
      [
        { "name": "selector_1", "data_category": "DEMOGRAPHIC" },
        { "name": "selector_1", "data_category": "DEMOGRAPHIC.TEST" }
      ]
      """
      for {
        resp <- res._1.server.run(put("configure/selectors", req, appToken(appId)))
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("fail adding already existing selectors") {
    res =>
      val req = json"""
      [
        { "name": "selector_1", "data_category": "DEMOGRAPHIC" }
      ]
      """
      for {
        resp1 <- res._1.server.run(put("configure/selectors", req, appToken(appId)))
        _     <- expect(resp1.status == Status.Ok).failFast
        resp2 <- res._1.server.run(put("configure/selectors", req, appToken(appId)))
        _     <- expect(resp2.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("get privacy scope dimenstions") {
    res =>
      for {
        resp <- res._1.server.run(get("configure/privacy-scope-dimensions", appToken(appId)))
        dims <- resp.to[PrivacyScopeDimensionsPayload]
        _    <- expect
          .all(
            dims.dataCategories.nonEmpty,
            dims.processingCategories.nonEmpty,
            dims.purposes.nonEmpty
          )
          .failFast
      } yield success
  }

  test("fail getting non existent legal base") {
    res =>
      for {
        resp <- res._1.server.run(get(s"configure/legal-bases/$uuid", appToken(appId)))
        _    <- expect(resp.status == Status.NotFound).failFast
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

  test("fail creating legal base with bad privacy scope") {
    res =>
      val req = json"""
      {
          "lb_type": "CONTRACT",
          "name": "test contract",
          "description": "",
          "scope": [
            {
              "data_categories": ["OTHER-DATA.fdsfsdafs"],
              "processing_categories": ["ANONYMIZATION"],
              "processing_purposes": ["JUSTICE"]
            }
          ]
      }
      """
      for {
        resp <- res._1.server.run(put("configure/legal-bases", req, appToken(appId)))
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("create and get contract legal base") {
    res =>
      val req = json"""
      {
          "lb_type": "CONTRACT",
          "name": "test contract",
          "description": "",
          "scope": [
            {
              "data_categories": ["BIOMETRIC"],
              "processing_categories": ["*"],
              "processing_purposes": ["SECURITY"]
            },
            {
              "data_categories": ["NAME"],
              "processing_categories": ["STORING", "PUBLISHING"],
              "processing_purposes": ["SECURITY", "SALE"]
            },
            {
              "data_categories": ["OTHER-DATA.PROOF", "BIOMETRIC"],
              "processing_categories": ["ANONYMIZATION"],
              "processing_purposes": ["JUSTICE"]
            }
          ]
      }
      """
      val t   = for {
        resp <- res._1.server.run(put("configure/legal-bases", req, appToken(appId)))
        _    <- expect(resp.status == Status.Ok).failFast
        lbId <- resp.as[String].map(_.uuid)
        _    <- waitUntilLbInserted(res._1.xa, lbId)
        resp <- res._1.server.run(get(s"configure/legal-bases/$lbId", appToken(appId)))
        lb   <- resp.to[LegalBase]
        _    <- res._2.legalBaseIds.update(lb.id :: _)
        _    <- expect
          .all(
            lb.id == lbId,
            lb.lbType == LegalBaseTerms.Contract,
            lb.scope == scope(
              ("OTHER-DATA.PROOF", "ANONYMIZATION", "JUSTICE"),
              ("BIOMETRIC", "ANONYMIZATION", "JUSTICE"),
              ("NAME", "STORING", "SECURITY"),
              ("NAME", "STORING", "SALE"),
              ("NAME", "PUBLISHING", "SECURITY"),
              ("NAME", "PUBLISHING", "SALE"),
              ("BIOMETRIC", "OTHER-PROCESSING", "SECURITY"),
              ("BIOMETRIC", "AUTOMATED-INFERENCE", "SECURITY"),
              ("BIOMETRIC", "COLLECTION", "SECURITY"),
              ("BIOMETRIC", "STORING", "SECURITY"),
              ("BIOMETRIC", "ANONYMIZATION", "SECURITY"),
              ("BIOMETRIC", "PUBLISHING", "SECURITY"),
              ("BIOMETRIC", "AUTOMATED-DECISION-MAKING", "SECURITY"),
              ("BIOMETRIC", "USING", "SECURITY"),
              ("BIOMETRIC", "GENERATING", "SECURITY"),
              ("BIOMETRIC", "SHARING", "SECURITY")
            ),
            lb.name == Some("test contract"),
            lb.description == Some(""),
            lb.active == true
          )
          .failFast
      } yield success
      t.guarantee(res._2.legalBasesAdded.release)
  }

  test("create and get consent legal base") {
    res =>
      val req = json"""
      {
          "lb_type": "CONSENT",
          "name": "test consent",
          "description": "",
          "scope": [
            {
              "data_categories": ["NAME"],
              "processing_categories": ["SHARING"],
              "processing_purposes": ["SERVICES"]
            }
          ]
      }
      """
      val t   = for {
        resp <- res._1.server.run(put("configure/legal-bases", req, appToken(appId)))
        _    <- expect(resp.status == Status.Ok).failFast
        lbId <- resp.as[String].map(_.uuid)
        _    <- waitUntilLbInserted(res._1.xa, lbId)
        resp <- res._1.server.run(get(s"configure/legal-bases/$lbId", appToken(appId)))
        lb   <- resp.to[LegalBase]
        _    <- res._2.legalBaseIds.update(lb.id :: _)
        _    <- expect
          .all(
            lb.id == lbId,
            lb.lbType == LegalBaseTerms.Consent,
            lb.scope == scope(
              ("NAME", "SHARING", "SERVICES.ADDITIONAL-SERVICES"),
              ("NAME", "SHARING", "SERVICES.BASIC-SERVICE")
            ),
            lb.name == Some("test consent"),
            lb.description == Some(""),
            lb.active == true
          )
          .failFast
      } yield success
      t.guarantee(res._2.legalBasesAdded.release)
  }

  test("create and get necessary legal base") {
    res =>
      val req = json"""
      {
          "lb_type": "NECESSARY",
          "name": "test necessary",
          "description": "",
          "scope": [
            {
              "data_categories": ["NAME"],
              "processing_categories": ["PUBLISHING"],
              "processing_purposes": ["SALE"]
            }
          ]
      }
      """
      val t   = for {
        resp <- res._1.server.run(put("configure/legal-bases", req, appToken(appId)))
        _    <- expect(resp.status == Status.Ok).failFast
        lbId <- resp.as[String].map(_.uuid)
        _    <- waitUntilLbInserted(res._1.xa, lbId)
        resp <- res._1.server.run(get(s"configure/legal-bases/$lbId", appToken(appId)))
        lb   <- resp.to[LegalBase]
        _    <- res._2.legalBaseIds.update(lb.id :: _)
        _    <- expect
          .all(
            lb.id == lbId,
            lb.lbType == LegalBaseTerms.Necessary,
            lb.scope == scope(("NAME", "PUBLISHING", "SALE")),
            lb.name == Some("test necessary"),
            lb.description == Some(""),
            lb.active == true
          )
          .failFast
      } yield success
      t.guarantee(res._2.legalBasesAdded.release)
  }

  test("create and get legitimate interest legal base") {
    res =>
      val req = json"""
      {
          "lb_type": "LEGITIMATE-INTEREST",
          "name": "test legitimate interest",
          "description": "",
          "scope": [
            {
              "data_categories": ["NAME"],
              "processing_categories": ["GENERATING"],
              "processing_purposes": ["RESEARCH"]
            }
          ]
      }
      """
      val t   = for {
        resp <- res._1.server.run(put("configure/legal-bases", req, appToken(appId)))
        _    <- expect(resp.status == Status.Ok).failFast
        lbId <- resp.as[String].map(_.uuid)
        _    <- waitUntilLbInserted(res._1.xa, lbId)
        resp <- res._1.server.run(get(s"configure/legal-bases/$lbId", appToken(appId)))
        lb   <- resp.to[LegalBase]
        _    <- res._2.legalBaseIds.update(lb.id :: _)
        _    <- expect
          .all(
            lb.id == lbId,
            lb.lbType == LegalBaseTerms.LegitimateInterest,
            lb.scope == scope(("NAME", "GENERATING", "RESEARCH")),
            lb.name == Some("test legitimate interest"),
            lb.description == Some(""),
            lb.active == true
          )
          .failFast
      } yield success
      t.guarantee(res._2.legalBasesAdded.release)
  }

  test("get all legal bases") {
    res =>
      for {
        _          <- res._2.legalBasesAdded.await
        resp       <- res._1.server.run(get(s"configure/legal-bases", appToken(appId)))
        lbs        <- resp.to[List[LegalBase]]
        savedLbIds <- res._2.legalBaseIds.get
        _          <- expect(lbs.map(_.id).sorted == savedLbIds.sorted).failFast
      } yield success
  }

  test("fail adding retention policies if none provided") {
    res =>
      val req = json"""[]"""
      for {
        resp <- res._1.server.run(put("configure/retention-policies", req, appToken(appId)))
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("fail adding retention policies for unknown data categories") {
    res =>
      val req = json"""
      [
        {
          "data_category": "AFFILIATION",
          "policy": "NO-LESS-THAN",
          "duration": "P5Y",
          "after": "SERVICE-START"
        },
        {
          "data_category": "AFFILIATION.TEST",
          "policy": "NO-LESS-THAN",
          "duration": "P5Y",
          "after": "SERVICE-END"
        }
      ]
      """
      for {
        resp <- res._1.server.run(put("configure/retention-policies", req, appToken(appId)))
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("create, get and delete retention policies") {
    res =>
      val req = json"""
      [
        {
          "data_category": "AFFILIATION",
          "policy": "NO-LESS-THAN",
          "duration": "P5Y",
          "after": "SERVICE-START"
        },
        {
          "data_category": "AFFILIATION",
          "policy": "NO-LONGER-THAN",
          "duration": "P1Y",
          "after": "SERVICE-END"
        },
        {
          "data_category": "BIOMETRIC",
          "policy": "NO-LESS-THAN",
          "duration": "P5Y",
          "after": "SERVICE-END"
        }
      ]
      """
      for {
        add <- res._1.server.run(put("configure/retention-policies", req, appToken(appId)))
        _   <- expect(add.status == Status.Ok).failFast

        get1 <- res._1.server.run(get("configure/data-categories", appToken(appId)))
        dcs1 <- get1.to[List[DataCategoryResponsePayload]]
        _    <- expect
          .all(
            dcs1.exists(
              dc =>
                dc.dataCategory.term == "AFFILIATION" &&
                  dc.retentionPolicies.length == 2 &&
                  // format: off
                  dc.retentionPolicies.exists(rp => rp.duration == "P5Y" && rp.policyType == RetentionPolicyTerms.NoLessThan && rp.after == EventTerms.ServiceStart) &&
                  dc.retentionPolicies.exists(rp => rp.duration == "P1Y" && rp.policyType == RetentionPolicyTerms.NoLongerThan && rp.after == EventTerms.ServiceEnd)
                  // format: on
            ),
            dcs1.exists(
              dc =>
                dc.dataCategory.term == "AFFILIATION.SCHOOL" &&
                  dc.retentionPolicies.length == 2 &&
                  // format: off
                  dc.retentionPolicies.exists(rp => rp.duration == "P5Y" && rp.policyType == RetentionPolicyTerms.NoLessThan && rp.after == EventTerms.ServiceStart) &&
                  dc.retentionPolicies.exists(rp => rp.duration == "P1Y" && rp.policyType == RetentionPolicyTerms.NoLongerThan && rp.after == EventTerms.ServiceEnd)
                  // format: on
            ),
            dcs1.exists(
              dc =>
                dc.dataCategory.term == "BIOMETRIC" &&
                  dc.retentionPolicies.length == 1 &&
                  // format: off
                  dc.retentionPolicies.exists(rp => rp.duration == "P5Y" && rp.policyType == RetentionPolicyTerms.NoLessThan && rp.after == EventTerms.ServiceEnd)
                  // format: on
            )
          )
          .failFast

        id = dcs1.find(_.dataCategory.term == "BIOMETRIC").get.retentionPolicies.head.id
        del <- res._1.server.run(delete(s"configure/retention-policies/$id", appToken(appId)))
        _   <- expect(del.status == Status.Ok).failFast

        get2 <- res._1.server.run(get("configure/data-categories", appToken(appId)))
        dcs2 <- get2.to[List[DataCategoryResponsePayload]]
        _    <- expect
          .all(
            dcs2
              .exists(dc => dc.dataCategory.term == "AFFILIATION" && dc.retentionPolicies.nonEmpty),
            dcs2.exists(dc => dc.dataCategory.term == "BIOMETRIC" && dc.retentionPolicies.isEmpty)
          )
          .failFast
      } yield success
  }

  test("fail adding provenances if none provided") {
    res =>
      val req = json"""[]"""
      for {
        resp <- res._1.server.run(put("configure/provenances", req, appToken(appId)))
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("fail adding provenances for unknown data categories") {
    res =>
      val req = json"""
      [
        {
          "data_category": "CONTACT",
          "provenance": "USER.DATA-SUBJECT",
          "system": "https://blindnet.io"
        },
        {
          "data_category": "CONTACT.TEST",
          "provenance": "DERIVED",
          "system": "https://blindnet.io"
        }
      ]
      """
      for {
        resp <- res._1.server.run(put("configure/provenances", req, appToken(appId)))
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("create, get and delete provenances") {
    res =>
      val req = json"""
      [
        {
          "data_category": "CONTACT",
          "provenance": "USER.DATA-SUBJECT",
          "system": "https://blindnet.io"
        },
        {
          "data_category": "CONTACT.EMAIL",
          "provenance": "DERIVED",
          "system": "https://blindnet.io"
        },
        {
          "data_category": "BIOMETRIC",
          "provenance": "TRANSFERRED",
          "system": "https://example.com"
        }
      ]
      """
      for {
        add <- res._1.server.run(put("configure/provenances", req, appToken(appId)))
        _   <- expect(add.status == Status.Ok).failFast

        get1 <- res._1.server.run(get("configure/data-categories", appToken(appId)))
        dcs1 <- get1.to[List[DataCategoryResponsePayload]]
        _    <- expect
          .all(
            dcs1.exists(
              dc =>
                dc.dataCategory.term == "CONTACT" &&
                dc.provenances.length == 1
                  // format: off
                  dc.provenances.exists(p => p.provenance == ProvenanceTerms.DataSubject && p.system == "https://blindnet.io")
                  // format: on
            ),
            dcs1.exists(
              dc =>
                dc.dataCategory.term == "CONTACT.EMAIL" &&
                  dc.provenances.length == 2 &&
                  // format: off
                  dc.provenances.exists(p => p.provenance == ProvenanceTerms.DataSubject && p.system == "https://blindnet.io") &&
                  dc.provenances.exists(p => p.provenance == ProvenanceTerms.Derived && p.system == "https://blindnet.io")
                  // format: on
            ),
            dcs1.exists(
              dc =>
                dc.dataCategory.term == "BIOMETRIC" &&
                  // format: off
                  dc.provenances.exists(p => p.provenance == ProvenanceTerms.Transferred && p.system == "https://example.com")
                  // format: on
            )
          )
          .failFast

        id = dcs1.find(_.dataCategory.term == "BIOMETRIC").get.provenances.head.id
        del <- res._1.server.run(delete(s"configure/provenances/$id", appToken(appId)))
        _   <- expect(del.status == Status.Ok).failFast

        get2 <- res._1.server.run(get("configure/data-categories", appToken(appId)))
        dcs2 <- get2.to[List[DataCategoryResponsePayload]]
        _    <- expect
          .all(
            dcs2
              .exists(dc => dc.dataCategory.term == "CONTACT" && dc.provenances.nonEmpty),
            dcs2.exists(dc => dc.dataCategory.term == "BIOMETRIC" && dc.provenances.isEmpty)
          )
          .failFast
      } yield success
  }

  test("get regulations") {
    res =>
      for {
        resp <- res._1.server.run(get("configure/regulations", appToken(appId)))
        regs <- resp.to[List[RegulationResponsePayload]]
        _    <- expect(
          regs.map(r => (r.name, r.description)).toSet == Set(("GDPR", Some("EU")), ("CCPA", None))
        ).failFast
      } yield success
  }

  test("fail adding regulations to app if none provided") {
    res =>
      val req = json"""{"regulation_ids": ${List(uuid)}}"""
      for {
        resp <- res._1.server.run(put("configure/regulations", req, appToken(appId)))
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("fail adding non-existent regulation to app") {
    res =>
      val req = json"""{"regulation_ids": []}"""
      for {
        resp <- res._1.server.run(put("configure/regulations", req, appToken(appId)))
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("add and delete regulation from app") {
    res =>
      for {
        getAllRegs <- res._1.server.run(get("configure/regulations", appToken(appId)))
        allRegs    <- getAllRegs.to[List[RegulationResponsePayload]]

        req = json"""{"regulation_ids":${allRegs.map(_.id)}}"""
        add <- res._1.server.run(put("configure/regulations", req, appToken(appId)))
        _   <- expect(add.status == Status.Ok).failFast

        getRegs1 <- res._1.server.run(get("configure/regulations/app", appToken(appId)))
        regs1    <- getRegs1.to[List[RegulationResponsePayload]]
        _        <- expect(
          regs1.map(r => (r.name, r.description)).toSet == Set(("GDPR", Some("EU")), ("CCPA", None))
        ).failFast

        ccpaId = allRegs.find(_.name == "CCPA").get.id
        del <- res._1.server.run(delete(s"configure/regulations/$ccpaId", appToken(appId)))
        _   <- expect(del.status == Status.Ok).failFast

        getRegs2 <- res._1.server.run(get("configure/regulations/app", appToken(appId)))
        regs2    <- getRegs2.to[List[RegulationResponsePayload]]
        _        <- expect(
          regs2.map(r => (r.name, r.description)).toSet == Set(("GDPR", Some("EU")))
        ).failFast

      } yield success
  }

  test("fail adding storage for non-existent app") {
    res =>
      val req = json"""{"url": "", "token": ""}"""
      for {
        resp <- res._1.server.run(put("configure/storage", req, appToken(uuid)))
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("add storage to app") {
    res =>
      val req = json"""{"url": "https://url", "token": "token_123"}"""
      for {
        resp <- res._1.server.run(put("configure/storage", req, appToken(appId)))
        _    <- expect(resp.status == Status.Ok).failFast

        storageConf <- sql"""select uri, token from dac where appid=$appId"""
          .query[(String, String)]
          .unique
          .transact(res._1.xa)

        _ <- expect(storageConf == ("https://url", "token_123")).failFast
      } yield success
  }

}
