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

class AdministrationEndpointsSuite(global: GlobalRead) extends IOSuite {

  val appId = uuid

  type Res = Resources
  def sharedResource: Resource[IO, Res] =
    for {
      res <- sharedResourceOrFallback(global)
      _   <- Resource.eval {
        for {
          _ <- dbutil.createApp(appId, res.xa)
        } yield ()
      }

    } yield res

  test("fail creating an application for existing id") {
    res =>
      val req = json"""
      {
          "app_id": $appId
      }
      """
      for {
        resp <- res.server.run(put("admin/applications", req, identityToken))
        _    <- expect(resp.status == Status.UnprocessableEntity).failFast
      } yield success
  }

  test("create an application") {
    res =>
      val id  = uuid
      val req = json"""
      {
          "app_id": $id
      }
      """
      for {
        resp <- res.server.run(put("admin/applications", req, identityToken))
        _    <- expect(resp.status == Status.Ok).failFast

        app <- sql"""select * from apps where id = $id"""
          .query[(UUID, Boolean)]
          .unique
          .transact(res.xa)
        _   <- expect(app == (id, true)).failFast

        rConfig <- sql"""select * from automatic_responses_config where appid = $id"""
          .query[(UUID, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean)]
          .unique
          .transact(res.xa)
        _       <- expect(rConfig == (id, true, true, true, true, true, true)).failFast

        dacConfig <- sql"""select * from dac where appid = $id"""
          .query[(UUID, Boolean, Option[String], Option[String])]
          .unique
          .transact(res.xa)
        _         <- expect(dacConfig == (id, false, None, None)).failFast

      } yield success
  }

}
