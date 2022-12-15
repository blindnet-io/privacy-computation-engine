package io.blindnet.pce
package config

import cats.Show
import cats.implicits.*
import ciris.*

case class DbConfig(
    uri: String,
    username: String,
    password: Secret[String]
)

object DbConfig {

  val load =
    (
      env("BN_DB_URI").as[String],
      env("BN_DB_USER").as[String],
      env("BN_DB_PASS").as[String].secret
    ).parMapN(DbConfig.apply)

  given Show[DbConfig] =
    Show.show(c => s"""|uri: ${c.uri}
                       |username: ${c.username}
                       |password: ${c.password.valueHash}""".stripMargin('|'))

}
