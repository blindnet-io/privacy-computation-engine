package io.blindnet.privacy
package model.vocabulary

import model.vocabulary.terms.*
import cats.kernel.Eq

case class PrivacyScopeTriple(
    dataCategories: DataCategory,
    processingCategories: ProcessingCategory,
    purpose: Purpose
) {
  def eql(other: PrivacyScopeTriple) = {
    dataCategories == other.dataCategories &&
    processingCategories == other.processingCategories &&
    purpose == other.purpose
  }

}

object PrivacyScopeTriple {
  def unsafe(dc: String, pc: String, pp: String) =
    PrivacyScopeTriple(DataCategory(dc), ProcessingCategory(pc), Purpose(pp))

  given Eq[PrivacyScopeTriple] = Eq.instance((a, b) => a eql b)
}
