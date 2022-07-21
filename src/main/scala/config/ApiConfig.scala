package io.blindnet.privacy
package config

import ciris.*
import com.comcast.ip4s.{ Ipv4Address, Port }
import cats.Show

case class ApiConfig(
    host: Ipv4Address,
    port: Port
)

given Show[ApiConfig] =
  Show.show(c => s"""|host: ${c.host.toString}
                     |port: ${c.port.value}""".stripMargin('|'))

given ConfigDecoder[String, Ipv4Address] =
  ConfigDecoder[String].mapOption("com.comcast.ip4s.Ipv4Address")(Ipv4Address.fromString)

given ConfigDecoder[String, Port] =
  ConfigDecoder[String].mapOption("com.comcast.ip4s.Port")(Port.fromString)
