package io.blindnet.pce
package priv

import cats.kernel.Eq
import terms.*

case class PrivacyScopeTriple(
    dataCategory: DataCategory,
    processingCategory: ProcessingCategory,
    purpose: Purpose
) {
  def eql(other: PrivacyScopeTriple) = {
    dataCategory == other.dataCategory &&
    processingCategory == other.processingCategory &&
    purpose == other.purpose
  }

}

object PrivacyScopeTriple {
  def unsafe(dc: String, pc: String, pp: String) =
    PrivacyScopeTriple(DataCategory(dc), ProcessingCategory(pc), Purpose(pp))

  given Eq[PrivacyScopeTriple] = Eq.instance((a, b) => a eql b)
}
