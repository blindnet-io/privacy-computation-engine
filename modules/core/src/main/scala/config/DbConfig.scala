package io.blindnet.pce
package config

import ciris.*
import cats.implicits.*
import cats.Show

case class DbConfig(
    uri: String,
    username: String,
    password: Secret[String]
)

object DbConfig {

  val load =
    (
      env("DB_URI").as[String],
      env("DB_USER").as[String],
      env("DB_PASS").as[String].secret
    ).parMapN(DbConfig.apply)

  given Show[DbConfig] =
    Show.show(c => s"""|uri: ${c.uri}
                       |username: ${c.username}
                       |password: ${c.password.valueHash}""".stripMargin('|'))

}
