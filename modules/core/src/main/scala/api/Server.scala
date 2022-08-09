package io.blindnet.pce
package api

import scala.concurrent.duration.*
import scala.language.postfixOps

import cats.effect.*
import cats.syntax.all.*
import com.comcast.ip4s.*
import org.http4s.HttpApp
import org.http4s.ember.server.*
import org.http4s.server.Server
import config.ApiConfig

object Server {

  def make(app: HttpApp[IO], conf: ApiConfig) =
    EmberServerBuilder
      .default[IO]
      .withHost(conf.host)
      .withPort(conf.port)
      .withHttpApp(app)
      .withIdleTimeout(1 minute)
      .build

}
