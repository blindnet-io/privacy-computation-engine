package io.blindnet.pce
package config

import ciris.*
import cats.Show
import cats.implicits.*
import org.http4s.Uri
import org.http4s.implicits.*
import io.blindnet.pce.config.util.{ *, given }

case class ComponentsConfig()

object ComponentsConfig {

  val load =
    default(ComponentsConfig())

  given Show[ComponentsConfig] =
    Show.show(c => s"""|
                       |""".stripMargin('|'))

}
