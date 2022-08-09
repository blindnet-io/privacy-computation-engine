package io.blindnet.pce
package config

import cats.implicits.*
import cats.effect.*
import ciris.*
import com.comcast.ip4s.*
import cats.Show

case class Config(
    env: AppEnvironment,
    db: DbConfig,
    api: ApiConfig
)

given Show[Config] =
  Show
    .show[Config](c => s"""
          |--------------------
          |CONFIGURATION
          | 
          |env: ${show"${c.env}"}
          |
          |db
          |${show"${c.db}"}
          |
          |api
          |${show"${c.api}"}
          |----------------------""".stripMargin('|'))

object Config {

  val loadApi =
    (
      env("API_HOST").as[Ipv4Address].default(ipv4"0.0.0.0"),
      env("API_PORT").as[Port].default(port"9000")
    ).parMapN(ApiConfig.apply)

  val loadDb =
    (
      env("DB_URI").as[String],
      env("DB_USER").as[String],
      env("DB_PASS").as[String].secret
    ).parMapN(DbConfig.apply)

  val load =
    (
      env("APP_ENV").as[AppEnvironment].default(AppEnvironment.Development),
      loadDb,
      loadApi
    )
      .parMapN(Config.apply)
      .load[IO]

}
