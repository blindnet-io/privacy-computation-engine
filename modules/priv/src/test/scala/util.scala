package io.blindnet.pce
package priv

import java.time.Instant
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.matchers.must.Matchers.*
import org.scalatest.funspec.*
import io.blindnet.pce.priv.terms.EventTerms
import io.blindnet.pce.priv.terms.LegalBaseTerms
import io.blindnet.pce.priv.terms.DataCategory
import scala.util.Random
import io.blindnet.pce.priv.terms.ProcessingCategory
import io.blindnet.pce.priv.terms.Purpose
import java.time.temporal.ChronoUnit
import java.util.UUID

object util {

  extension [T](l: List[T]) def sample = l(Random.nextInt(l.length))

  extension (i: Instant) def -(n: Int) = i.minus(1, ChronoUnit.DAYS)

  def uuid = java.util.UUID.randomUUID

  def scope(t: (String, String, String)*) = PrivacyScope(
    t.toSet.map(tt => PrivacyScopeTriple.unsafe(tt._1, tt._2, tt._3))
  )

}

object PS {
  val full = {
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
