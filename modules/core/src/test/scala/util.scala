package io.blindnet.pce

import java.time.Instant
import scala.util.Random
import java.time.temporal.ChronoUnit
import java.util.UUID
import io.blindnet.pce.priv.privacyrequest.*
import io.circe.Json
import io.blindnet.pce.priv.*

object testutil {

  extension [T](l: List[T]) def sample = l(Random.nextInt(l.length))

  extension (i: Instant) def -(n: Int) = i.minus(1, ChronoUnit.DAYS)

  def uuid = java.util.UUID.randomUUID

  def now = Instant.now()

  def scope(t: (String, String, String)*) = PrivacyScope(
    t.toSet.map(tt => PrivacyScopeTriple.unsafe(tt._1, tt._2, tt._3))
  )

}
