package io.blindnet.pce
package priv
package terms

import cats.data.Validated
import doobie.util.Get
import io.circe.*
import sttp.tapir.*

enum Motive(term: String) {
  case IdentityUnconfirmed extends Motive("IDENTITY-UNCONFIRMED")
  case LanguageUnsupported extends Motive("LANGUAGE-UNSUPPORTED")
  case ValidReasons        extends Motive("VALID-REASONS")
  case Impossible          extends Motive("IMPOSSIBLE")
  case NoSuchData          extends Motive("NO-SUCH-DATA")
  case RequestUnsupported  extends Motive("REQUEST-UNSUPPORTED")
  case UserUnknown         extends Motive("USER-UNKNOWN")
  case OtherMotive         extends Motive("OTHER-MOTIVE")

  def isTerm(str: String) = term == str

  val encode = term
}

object Motive {
  def parse(str: String): Validated[String, Motive] =
    Validated.fromOption(
      Motive.values.find(a => a.isTerm(str)),
      "Unknown motive term"
    )

  def parseUnsafe(str: String): Motive =
    Motive.values.find(a => a.isTerm(str)).get

  given Decoder[Motive] =
    Decoder.decodeString.emap(Motive.parse(_).toEither)

  given Encoder[Motive] =
    Encoder[String].contramap(_.encode)

  given Schema[Motive] =
    Schema.string.validate(Validator.enumeration(Motive.values.toList, x => Option(x.encode)))

  given Get[Motive] =
    Get[String].tmap(t => Motive.parseUnsafe(t))

}
