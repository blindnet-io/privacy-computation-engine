package io.blindnet.pce
package config

import cats.implicits.*
import cats.effect.*
import ciris.*
import org.http4s.implicits.*
import com.comcast.ip4s.*
import cats.Show
import org.http4s.Uri
import io.blindnet.pce.config.util.{ *, given }

case class Config(
    env: AppEnvironment,
    callbackUri: Uri,
    db: DbConfig,
    api: ApiConfig,
    components: ComponentsConfig
)

given Show[Config] =
  Show
    .show[Config](c => s"""
          |--------------------
          |CONFIGURATION
          | 
          |env: ${show"${c.env}"}
          |callback uri: ${show"${c.callbackUri}"}
          |
          |db
          |${show"${c.db}"}
          |
          |api
          |${show"${c.api}"}
          |
          |components
          |${show"${c.components}"}
          |----------------------""".stripMargin('|'))

object Config {

  val load =
    (
      env("APP_ENV").as[AppEnvironment].default(AppEnvironment.Development),
      env("APP_CALLBACK_URI").as[Uri].default(uri"localhost"),
      DbConfig.load,
      ApiConfig.load,
      ComponentsConfig.load
    )
      .parMapN(Config.apply)
      .load[IO]

}
