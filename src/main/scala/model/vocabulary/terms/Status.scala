package io.blindnet.privacy
package model.vocabulary.terms

import cats.data.Validated
import io.circe.*
import sttp.tapir.*

enum Status(term: String) {
  case Granted          extends Status("GRANTED")
  case Denied           extends Status("DENIED")
  case PartiallyGranted extends Status("PARTIALLY-GRANTED")
  case UnderReview      extends Status("UNDER-REVIEW")
  case Canceled         extends Status("CANCELED")

  def isTerm(str: String) = term == str

  val encode = term
}

object Status {
  def parse(str: String): Validated[String, Status] =
    Validated.fromOption(
      Status.values.find(a => a.isTerm(str)),
      "Unknown status term"
    )

  given Decoder[Status] =
    Decoder.decodeString.emap(Status.parse(_).toEither)

  given Encoder[Status] =
    Encoder[String].contramap(_.encode)

  given Schema[Status] =
    Schema.string.validate(Validator.enumeration(Status.values.toList, x => Option(x.encode)))

}
