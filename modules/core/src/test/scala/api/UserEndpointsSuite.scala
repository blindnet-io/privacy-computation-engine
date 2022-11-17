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
import api.endpoints.messages.user.*
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

class UserEndpointsSuite(global: GlobalRead) extends IOSuite {

  val appId = uuid
  val ds    = DataSubject(uuid.toString, appId)

  val consent1 = uuid
  val consent2 = uuid
  val consent3 = uuid

  type Res = Resources
  def sharedResource: Resource[IO, Resources] =
    for {
      res <- global.getOrFailR[Resources]()

      _ <- Resource.eval {
        for {
          _ <- createApp(appId, res.xa)
          _ <- createDs(ds.id, appId, res.xa)

          _ <- createLegalBase(consent1, appId, LegalBaseTerms.Consent, "Prizes consent", res.xa)
          _ <- createLegalBase(consent2, appId, LegalBaseTerms.Consent, "test consent 1", res.xa)
          _ <- createLegalBase(consent3, appId, LegalBaseTerms.Consent, "test consent 2", res.xa)

          _ <- sql"""
          insert into consent_given_events values
            ($uuid, $consent1, ${ds.id}, $appId, ${now - 10}),
            ($uuid, $consent2, ${ds.id}, $appId, ${now - 9}),
            ($uuid, $consent1, ${ds.id}, $appId, ${now - 8}),
            ($uuid, $consent3, ${ds.id}, $appId, ${now - 7}),
            ($uuid, $consent2, ${ds.id}, $appId, ${now - 6})
          """.update.run.transact(res.xa)
          _ <- sql"""
          insert into consent_revoked_events values
            ($uuid, $consent2, ${ds.id}, $appId, ${now - 11}),
            ($uuid, $consent2, ${ds.id}, $appId, ${now - 7}),
            ($uuid, $consent3, ${ds.id}, $appId, ${now - 5})
          """.update.run.transact(res.xa)
        } yield ()
      }
    } yield res

  test("return users active consents") {
    res =>
      for {

        resp     <- res.server.run(get("user/consents", userToken(appId, ds.id)))
        consents <- resp.to[List[GivenConsentsPayload]]
        _        <- expect(consents.map(_.id).sorted == List(consent1, consent2).sorted).failFast
      } yield success
  }

}
