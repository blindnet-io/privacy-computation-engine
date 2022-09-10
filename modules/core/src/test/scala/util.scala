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

object testutil {

  extension [T](l: List[T]) def sample = l(Random.nextInt(l.length))

  extension (i: Instant) def -(n: Int) = i.minus(1, ChronoUnit.DAYS)

  def uuid = java.util.UUID.randomUUID

  def now = Instant.now()

  def scope(t: (String, String, String)*) = PrivacyScope(
    t.toSet.map(tt => PrivacyScopeTriple.unsafe(tt._1, tt._2, tt._3))
  )

}

object httputil {

  // TODO: add auth
  def req(method: Method, path: String) =
    Request[IO]()
      .withUri(uri"/v0".addPath(path))
      .withMethod(method)

  def get(path: String) =
    req(Method.GET, path)

  def post(path: String, body: Json) =
    req(Method.POST, path).withEntity(body)

  def put(path: String, body: String) =
    req(Method.PUT, path).withEntity(body)

  def delete(path: String, body: String) =
    req(Method.DELETE, path)

}
