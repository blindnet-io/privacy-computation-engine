package io.blindnet.pce
package priv

import java.time.Instant
import scala.util.Random
import io.blindnet.pce.priv.terms.*
import java.time.temporal.ChronoUnit
import java.util.UUID
import io.blindnet.pce.priv.privacyrequest.*
import io.circe.Json

object util {

  extension [T](l: List[T]) def sample = l(Random.nextInt(l.length))

  extension (i: Instant) def -(n: Int) = i.minus(1, ChronoUnit.DAYS)

  def uuid = java.util.UUID.randomUUID

  def now = Instant.now()

  def scope(t: (String, String, String)*) = PrivacyScope(
    t.toSet.map(tt => PrivacyScopeTriple.unsafe(tt._1, tt._2, tt._3))
  )

}

import util.*

trait request {
  val appId = uuid
  def request(
      demands: List[Demand],
      t: Instant = now,
      ds: Option[DataSubject] = None,
      dsIds: List[String] = List.empty,
      tg: Target = Target.System,
      email: Option[String] = None
  ) = PrivacyRequest(RequestId(uuid), appId, t, tg, email, ds, dsIds, demands)

}

trait demand {
  def demand(
      a: Action,
      r: List[Restriction] = List.empty,
      m: Option[String] = None,
      l: Option[String] = None,
      d: List[String] = List.empty
  ): Demand =
    Demand(uuid, RequestId(uuid), a, m, l, d, r)

}

trait restriction {
  // format: off
  def ps(ps: PrivacyScope = PrivacyScope.empty): Restriction.PrivacyScope = Restriction.PrivacyScope(ps)
  def consent(id: UUID = uuid): Restriction.Consent = Restriction.Consent(id)
  def date(from: Option[Instant] = None, to: Option[Instant] = None): Restriction.DateRange = Restriction.DateRange(from, to)
  def prov(p: ProvenanceTerms = ProvenanceTerms.User, t: Option[Target] = None): Restriction.Provenance = Restriction.Provenance(p, t)
  def dataRef(drs: List[String] = List.empty): Restriction.DataReference = Restriction.DataReference(drs)
  // format: on
}

trait response {
  def response(
      dId: UUID = uuid,
      t: Instant = now,
      a: Action = Action.Access,
      s: Status = Status.Granted,
      motive: Option[Motive] = None,
      answer: Option[Json] = None,
      message: Option[String] = None,
      lang: Option[String] = None,
      system: Option[String] = None,
      parent: Option[ResponseId] = None,
      includes: List[PrivacyResponse] = List.empty,
      data: Option[String] = None
  ) =
      // format: off
      PrivacyResponse(ResponseId(uuid), ResponseEventId(uuid), dId, t, a, s, motive, answer, message, lang, system, parent, includes, data)
      // format: off
}

trait PS {
  val fullPS = {
    val triples = for {
      dc <- DataCategory.terms.flatMap(dc => DataCategory.granularize(DataCategory(dc)))
      pc <- ProcessingCategory.terms.flatMap(
        pc => ProcessingCategory.granularize(ProcessingCategory(pc))
      )
      pp <- Purpose.terms.flatMap(pp => Purpose.granularize(Purpose(pp)))
    } yield PrivacyScopeTriple(dc, pc, pp)
    PrivacyScope(triples.toSet)
  }

}

object test extends request with demand with restriction with response with PS
