package io.blindnet.pce
package config

import cats.Show
import cats.implicits.*
import ciris.*

case class TokensConfig(
    identity: Secret[String]
)

object TokensConfig {

  val load =
    env("BN_TOKEN_IDENTITY").as[String].secret.map(TokensConfig.apply)

  given Show[TokensConfig] =
    Show.show(c => s"""|identity: ${c.identity.valueHash}""".stripMargin('|'))

}
