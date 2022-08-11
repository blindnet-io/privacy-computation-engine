package io.blindnet.pce
package config

import ciris.*
import cats.Show
import cats.implicits.*
import org.http4s.Uri
import org.http4s.implicits.*
import io.blindnet.pce.config.util.{ *, given }

case class DataAccessComponentConfig(
    uri: Uri
)

object DataAccessComponentConfig {

  given Show[DataAccessComponentConfig] =
    Show.show(c => s"""|uri: ${c.uri}""".stripMargin('|'))

}

case class ComponentsConfig(
    dac: DataAccessComponentConfig
)

object ComponentsConfig {

  val load =
    env("DCA_URI")
      .as[Uri]
      .default(uri"localhost")
      .map(dcaUri => ComponentsConfig(DataAccessComponentConfig(dcaUri)))

  given Show[ComponentsConfig] =
    Show.show(c => s"""|data access component
                       |${show"${c.dac}"}""".stripMargin('|'))

}
