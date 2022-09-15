package io.blindnet.pce
package config

import cats.Show
import cats.implicits.*
import ciris.*
import io.blindnet.pce.config.util.{ *, given }
import org.http4s.Uri
import org.http4s.implicits.*

case class ComponentsConfig()

object ComponentsConfig {

  val load =
    default(ComponentsConfig())

  given Show[ComponentsConfig] =
    Show.show(c => s"""|
                       |""".stripMargin('|'))

}
