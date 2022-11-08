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

object testutil {

  extension [T](l: List[T]) def sample = l(Random.nextInt(l.length))

  extension (i: Instant) def -(n: Int) = i.minus(n, ChronoUnit.DAYS)

  extension (s: String) def uuid = UUID.fromString(s)

  def uuid = java.util.UUID.randomUUID

  def now = Instant.now()

  def scope(t: (String, String, String)*) = PrivacyScope(
    t.toSet.map(tt => PrivacyScopeTriple.unsafe(tt._1, tt._2, tt._3))
  )

  val appId = "6f083c15-4ada-4671-a6d1-c671bc9105dc".uuid
  val ds    = DataSubject("fdfc95a6-8fd8-4581-91f7-b3d236a6a10e", appId)

  val secretKey = TokenPrivateKey.generateRandom()
  val publicKey = secretKey.toPublicKey().toString()
  val tb        = TokenBuilder(appId, secretKey)
  val appToken  = tb.app()
  val userToken = tb.user(ds.id)
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

  def post(path: String, body: Json, token: Option[String] = None) =
    req(Method.POST, path, token).withEntity(body)

  def put(path: String, body: String, token: Option[String] = None) =
    req(Method.PUT, path, token).withEntity(body)

  def delete(path: String, body: String, token: Option[String] = None) =
    req(Method.DELETE, path, token)

}
