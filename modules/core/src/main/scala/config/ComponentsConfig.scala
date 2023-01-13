package io.blindnet.pce
package config

import cats.Show
import cats.implicits.*
import ciris.*
import io.blindnet.pce.config.util.{ *, given }
import org.http4s.Uri
import org.http4s.implicits.*
import cats.effect.IO

case class ComponentsConfig(
    identityUrl: Uri,
    identityKey: String
)

object ComponentsConfig {

  val load =
    (
      env("BN_IDENTITY_URL").as[Uri],
      env("BN_IDENTITY_KEY").as[String]
    )
      .parMapN(ComponentsConfig.apply)

  given Show[ComponentsConfig] =
    Show.show(c => s"""|identity url: ${c.identityUrl}
                       |identity key: ${c.identityKey}""".stripMargin('|'))

}
