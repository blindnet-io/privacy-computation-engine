package io.blindnet.privacy
package api

import scala.concurrent.duration.*
import scala.language.postfixOps

import cats.effect.*
import cats.syntax.all.*
import com.comcast.ip4s.*
import org.http4s.HttpApp
import org.http4s.ember.server.*
import org.http4s.server.Server

object Server {

  def make(app: HttpApp[IO]) =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(app)
      .withIdleTimeout(1 minute)
      .build

}
