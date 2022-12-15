package io.blindnet.pce
package config

import cats.Show
import cats.implicits.*
import ciris.*
import io.blindnet.pce.config.util.{ *, given }
import org.http4s.Uri
import org.http4s.implicits.*

case class ComponentsConfig(
    identityUrl: Uri
)

object ComponentsConfig {

  val load =
    env("BN_IDENTITY_URL").as[Uri].map(ComponentsConfig.apply)

  given Show[ComponentsConfig] =
    Show.show(c => s"""|identity_url: ${c.identityUrl}""".stripMargin('|'))

}
