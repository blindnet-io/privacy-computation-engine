package io.blindnet.pce
package priv

import io.circe.*
import io.circe.generic.semiauto.*
import sttp.tapir.Schema
import cats.Show
import cats.implicits.*
import io.blindnet.pce.priv.terms.*

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

  def zoomIn(selectors: List[DataCategory] = List.empty) = {
    val newTriples = triples.flatMap(
      triple =>
        for {
          dc <- DataCategory.getMostGranular(triple.dataCategory, selectors)
          pc <- ProcessingCategory.getMostGranular(triple.processingCategory)
          pp <- Purpose.getMostGranular(triple.purpose)
        } yield PrivacyScopeTriple(dc, pc, pp)
    )
    PrivacyScope(newTriples)
  }

  def zoomOut() = ???
}

object PrivacyScope {
  def empty = PrivacyScope(Set.empty)

  given Show[PrivacyScope] =
    Show.show(_.triples.grouped(3).map(g => g.map(t => show"$t").mkString(" ")).mkString("\n"))

  given Decoder[PrivacyScope] = deriveDecoder[PrivacyScope]
  given Encoder[PrivacyScope] = deriveEncoder[PrivacyScope]

  given Schema[PrivacyScope] = Schema.derived[PrivacyScope]

}
