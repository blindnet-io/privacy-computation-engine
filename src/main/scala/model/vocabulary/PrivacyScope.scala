package io.blindnet.privacy
package model.vocabulary

import model.vocabulary.terms.*
import cats.kernel.Eq

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

}

object PrivacyScope {
  def empty = PrivacyScope(Set.empty)
}
