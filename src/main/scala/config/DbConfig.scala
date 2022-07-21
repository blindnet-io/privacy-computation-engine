package io.blindnet.privacy
package config

import cats.Show
import ciris.Secret

case class DbConfig(
    uri: String,
    username: String,
    password: Secret[String]
)

given Show[DbConfig] =
  Show.show(c => s"""|uri: ${c.uri}
                     |username: ${c.username}
                     |password: ${c.password.valueHash}""".stripMargin('|'))
