package io.blindnet.privacy
package model.vocabulary.terms

import cats.data.Validated
import cats.implicits.*
import io.circe.*

enum BooleanTerms {
  case Yes
  case No
}

object BooleanTerms {
  def parse(s: String): Validated[String, BooleanTerms] =
    s match {
      case "YES" => BooleanTerms.Yes.valid
      case "NO"  => BooleanTerms.No.valid
      case _     => "Unknown boolean term".invalid
    }

  given Decoder[BooleanTerms] =
    Decoder.decodeString.emap(BooleanTerms.parse(_).toEither)

  given Encoder[BooleanTerms] =
    Encoder[String].contramap(_ match {
      case Yes => "YES"
      case No  => "NO"
    })

}
