package io.blindnet.pce
package priv

import cats.Show
import cats.implicits.*
import cats.kernel.Monoid
import io.blindnet.pce.priv.terms.*
import io.circe.*
import io.circe.generic.semiauto.*
import sttp.tapir.Schema

// TODO: optimize methods
case class PrivacyScope(
    triples: Set[PrivacyScopeTriple]
) {
  def union(other: PrivacyScope) =
    this.copy(triples union other.triples)

  def intersection(other: PrivacyScope) =
    this.copy(triples intersect other.triples)

  def difference(other: PrivacyScope) =
    this.copy(triples diff other.triples)

  def isEmpty = triples.isEmpty

  def zoomIn(ctx: PSContext = PSContext.empty): PrivacyScope = {
    val newTriples = triples.flatMap(
      triple =>
        for {
          dc <- DataCategory.granularize(triple.dataCategory, ctx.selectors)
          pc <- ProcessingCategory.granularize(triple.processingCategory)
          pp <- Purpose.granularize(triple.purpose)
        } yield PrivacyScopeTriple(dc, pc, pp)
    )
    PrivacyScope(newTriples)
  }

  def zoomOut() = ???

  val dataCategories: Set[DataCategory] = triples.map(_.dataCategory)
}

object PrivacyScope {
  def empty = PrivacyScope(Set.empty)

  def full(ctx: PSContext = PSContext.empty) =
    PrivacyScope(Set(PrivacyScopeTriple(DataCategory.All, ProcessingCategory.All, Purpose.All)))
      .zoomIn(ctx)

  def unsafe(dcs: Seq[String], pcs: Seq[String], pps: Seq[String]) =
    PrivacyScope((dcs lazyZip pcs lazyZip pps).map(PrivacyScopeTriple.unsafe).toSet)

  def validate(ps: PrivacyScope, ctx: PSContext) = {
    ps.triples.forall(
      t =>
        DataCategory.terms.contains(t.dataCategory.term) || ctx.selectors.contains(t.dataCategory)
    )
  }

  given Monoid[PrivacyScope] =
    Monoid.instance(empty, (ps1, ps2) => ps1 union ps2)

  given Show[PrivacyScope] =
    Show.show(_.triples.grouped(3).map(g => g.map(t => show"$t").mkString(" ")).mkString("\n"))

  given Decoder[PrivacyScope] = deriveDecoder[PrivacyScope]
  given Encoder[PrivacyScope] = deriveEncoder[PrivacyScope]

  given Schema[PrivacyScope] = Schema.derived[PrivacyScope]

}
