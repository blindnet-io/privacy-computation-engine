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
import SharedResources.*
import testutil.*
import httputil.*

class UserEndpointsSuite(global: GlobalRead) extends IOSuite {

  type Res = Resources
  def sharedResource: Resource[IO, Resources] = sharedResourceOrFallback(global)

  val consent1 = "28b5bee0-9db8-40ec-840e-64eafbfb9ddd".uuid
  val consent2 = "b25c1c0c-d375-4a5c-8500-6918f2888435".uuid
  val consent3 = "b52f8b4b-590c-4dcb-b572-f4a890ea330b".uuid

  test("return users active consents") {
    res =>
      val uid   = uuid
      val token = tb().user(uid.toString)
      for {
        _ <- sql"""insert into data_subjects values ($uid, $appId)""".update.run.transact(res.xa)
        _ <- sql"""
          insert into consent_given_events values
            ($uuid, $consent1, $uid, $appId, ${now - 10}),
            ($uuid, $consent2, $uid, $appId, ${now - 9}),
            ($uuid, $consent1, $uid, $appId, ${now - 8}),
            ($uuid, $consent3, $uid, $appId, ${now - 7}),
            ($uuid, $consent2, $uid, $appId, ${now - 6})
          """.update.run.transact(res.xa)
        _ <- sql"""
          insert into consent_revoked_events values
            ($uuid, $consent2, $uid, $appId, ${now - 11}),
            ($uuid, $consent2, $uid, $appId, ${now - 7}),
            ($uuid, $consent3, $uid, $appId, ${now - 5})
          """.update.run.transact(res.xa)

        resp     <- res.server.run(get("user/consents", Some(token)))
        consents <- resp.to[List[GivenConsentsPayload]]
        _        <- expect(consents.map(_.id).sorted == List(consent2, consent1)).failFast
      } yield success
  }

}
