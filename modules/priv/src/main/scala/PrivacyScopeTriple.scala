package io.blindnet.pce
package priv

import cats.Show
import cats.kernel.Eq
import io.circe.Decoder.Result
import io.circe.*
import sttp.tapir.Schema
import sttp.tapir.generic.Configuration
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

  given Show[PrivacyScopeTriple] =
    Show.show(t => s"{ ${t.dataCategory.term}  ${t.processingCategory.term}  ${t.purpose.term} }")

  given Decoder[PrivacyScopeTriple] = new Decoder {
    def apply(c: HCursor): Result[PrivacyScopeTriple] =
      for {
        dc <- c.downField("dc").as[DataCategory]
        pc <- c.downField("pc").as[ProcessingCategory]
        pp <- c.downField("pp").as[Purpose]
        res = PrivacyScopeTriple(dc, pc, pp)
      } yield res

  }

  given Encoder[PrivacyScopeTriple] = Encoder.forProduct3("dc", "pc", "pp")(
    ps => (ps.dataCategory.term, ps.processingCategory.term, ps.purpose.term)
  )

  // TODO: custom fields
  given Schema[PrivacyScopeTriple] =
    Schema.derived[PrivacyScopeTriple](using Configuration.default.withSnakeCaseMemberNames)

}
