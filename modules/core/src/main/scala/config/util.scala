package io.blindnet.pce
package config

import cats.Show
import cats.implicits.*
import ciris.*
import com.comcast.ip4s.*
import org.http4s.Uri

object util {

  given ConfigDecoder[String, Uri] =
    ConfigDecoder[String].mapOption("org.http4s.Uri")(Uri.fromString(_).toOption)

  given ConfigDecoder[String, Ipv4Address] =
    ConfigDecoder[String].mapOption("com.comcast.ip4s.Ipv4Address")(Ipv4Address.fromString)

  given ConfigDecoder[String, Port] =
    ConfigDecoder[String].mapOption("com.comcast.ip4s.Port")(Port.fromString)

}
