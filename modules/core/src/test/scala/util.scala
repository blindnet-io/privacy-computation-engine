package io.blindnet.pce

import java.time.Instant
import scala.util.Random
import java.time.temporal.ChronoUnit
import java.util.UUID
import io.blindnet.pce.priv.privacyrequest.*
import io.circe.Json
import io.blindnet.pce.priv.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import io.circe.*
import io.circe.parser.*
import io.circe.literal.*
import cats.effect.IO
import org.http4s.headers.Authorization
import org.typelevel.ci.*
import io.blindnet.jwt.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import io.blindnet.pce.priv.terms.LegalBaseTerms

object testutil {

  extension [T](l: List[T]) def sample = l(Random.nextInt(l.length))

  extension (i: Instant)
    def -(n: Int) = i.minus(n, ChronoUnit.DAYS)
    def +(n: Int) = i.plus(n, ChronoUnit.DAYS)

  extension (s: String) def uuid = UUID.fromString(s)

  extension (r: Response[IO]) def to[T: Decoder] = r.asJson.map(_.as[T].toOption.get)

  def uuid = java.util.UUID.randomUUID

  def now = Instant.now()

  def scope(t: (String, String, String)*) = PrivacyScope(
    t.toSet.map(tt => PrivacyScopeTriple.unsafe(tt._1, tt._2, tt._3))
  )

  val secretKey                              = TokenPrivateKey.generateRandom()
  val publicKey                              = secretKey.toPublicKey().toString()
  def tb(appId: UUID)                        = TokenBuilder(appId, secretKey)
  def appToken(appId: UUID)                  = tb(appId).app()
  def userToken(appId: UUID, userId: String) = tb(appId).user(userId)
}

object httputil {

  def req(method: Method, path: String, token: Option[String] = None) =
    Request[IO]()
      .withUri(uri"/v0".addPath(path))
      .withMethod(method)
      .putHeaders(
        token.map(t => List(Header.Raw(ci"Authorization", s"Bearer $t"))).getOrElse(List.empty)
      )

  def get(path: String, token: Option[String] = None) =
    req(Method.GET, path, token)

  def get(path: String, token: String) =
    req(Method.GET, path, Some(token))

  def post(path: String, body: Json, token: Option[String] = None) =
    req(Method.POST, path, token).withEntity(body)

  def post(path: String, body: Json, token: String) =
    req(Method.POST, path, Some(token)).withEntity(body)

  def put(path: String, body: Json, token: Option[String] = None) =
    req(Method.PUT, path, token).withEntity(body)

  def put(path: String, body: Json, token: String) =
    req(Method.PUT, path, Some(token)).withEntity(body)

  def delete(path: String, token: Option[String] = None) =
    req(Method.DELETE, path, token)

  def delete(path: String, token: String) =
    req(Method.DELETE, path, Some(token))

}

object dbutil {
  def createApp(id: UUID, xa: Transactor[IO]) =
    for {
      _ <- sql"""insert into apps values ($id)""".update.run.transact(xa)
      _ <- sql"""insert into dac values ($id, false, '')""".update.run.transact(xa)
      _ <- sql"""
      insert into automatic_responses_config values
      ($id, true, true, true, true)
      """.update.run.transact(xa)
      _ <- sql"""
      insert into general_information values
      (${testutil.uuid}, $id, 'test', 'dpo@fakemail.me', array ['France', 'USA'], array ['dc cat 1', 'dc cat 2'], array ['policy 1', 'policy 2'], 'https://blindnet.io/privacy', 'your data is secure')
      """.update.run.transact(xa)
    } yield ()

  def createDs(id: String, appId: UUID, xa: Transactor[IO]) =
    for {
      _ <- sql"""insert into data_subjects values ($id, $appId, 'id')""".update.run.transact(xa)
    } yield ()

  def createLegalBase(
      id: UUID,
      appId: UUID,
      typ: LegalBaseTerms,
      name: String,
      xa: Transactor[IO]
  ) =
    for {
      _ <- sql"""
        insert into legal_bases values
        ($id, $appId, ${typ.encode}::legal_base_terms, $name, '', true)
        """.update.run.transact(xa)
    } yield ()

  def createRegulations(xa: Transactor[IO]) =
    for {
      _ <- sql"""
      insert into regulations values (${testutil.uuid}, 'GDPR', 'EU'), (${testutil.uuid}, 'CCPA', null)
      """.update.run.transact(xa)
    } yield ()

}
