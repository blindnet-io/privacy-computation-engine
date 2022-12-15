package io.blindnet.pce
package config

import cats.Show
import cats.implicits.*
import ciris.*

case class RedisConfig(
    uri: Secret[String]
)

object RedisConfig {

  val load =
    env("BN_REDIS_URI").as[String].secret.map(RedisConfig.apply)

  given Show[RedisConfig] =
    Show.show(c => s"""|uri: ${c.uri.valueHash}""".stripMargin('|'))

}
