package io.blindnet.pce
package config

import cats.Show
import cats.implicits.*
import ciris.*
import com.comcast.ip4s.*
import io.blindnet.pce.config.util.{ *, given }

case class ApiConfig(
    host: Ipv4Address,
    port: Port
)

object ApiConfig {

  val load =
    (
      env("BN_API_HOST").as[Ipv4Address].default(ipv4"0.0.0.0"),
      env("BN_API_PORT").as[Port].default(port"9000")
    ).parMapN(ApiConfig.apply)

  given Show[ApiConfig] =
    Show.show(c => s"""|host: ${c.host.toString}
                       |port: ${c.port.value}""".stripMargin('|'))

}
