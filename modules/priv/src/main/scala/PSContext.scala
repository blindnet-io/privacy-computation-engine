package io.blindnet.pce
package priv

import io.blindnet.pce.priv.terms.*

case class PSContext(
    selectors: Set[DataCategory]
)

object PSContext {
  def empty = PSContext(Set.empty)
}
