package io.blindnet.pce
package config

import cats.Show
import cats.effect.*
import cats.implicits.*
import ciris.*
import com.comcast.ip4s.*
import io.blindnet.pce.config.util.{ *, given }
import org.http4s.Uri
import org.http4s.implicits.*

case class Config(
    env: AppEnvironment,
    callbackUri: Uri,
    db: DbConfig,
    redis: RedisConfig,
    api: ApiConfig,
    tokens: TokensConfig,
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
          |redis
          |${show"${c.redis}"}
          |
          |api
          |${show"${c.api}"}
          |
          |tokens
          |${show"${c.tokens}"}
          |
          |components
          |${show"${c.components}"}
          |
          |----------------------""".stripMargin('|'))

object Config {

  val load =
    (
      env("BN_APP_ENV").as[AppEnvironment].default(AppEnvironment.Development),
      env("BN_APP_CALLBACK_URI").as[Uri].default(uri"localhost:9000/v0"),
      DbConfig.load,
      RedisConfig.load,
      ApiConfig.load,
      TokensConfig.load,
      ComponentsConfig.load
    )
      .parMapN(Config.apply)
      .load[IO]

}
