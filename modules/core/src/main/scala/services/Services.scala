package io.blindnet.pce
package services

import cats.effect.*
import cats.effect.std.*
import io.blindnet.pce.services.*
import org.http4s.client.Client
import db.repositories.Repositories
import config.Config

trait Services {
  val privacyRequest: PrivacyRequestService
  val consumerInterface: DataConsumerInterfaceService
  val configuration: ConfigurationService
  val userEvents: UserEventsService
  val user: UserService
  val callbacks: CallbackHandler
}

object Services {
  def make(
      repos: Repositories,
      conf: Config
  ) = {
    lazy val privacyRequestService    = PrivacyRequestService(repos)
    lazy val consumerInterfaceService = DataConsumerInterfaceService(repos)
    lazy val configurationService     = ConfigurationService(repos)
    lazy val userEventsService        = UserEventsService(repos)
    lazy val userService              = UserService(repos)
    lazy val callbackHandler          = CallbackHandler(repos)

    new Services {
      val privacyRequest    = privacyRequestService
      val consumerInterface = consumerInterfaceService
      val configuration     = configurationService
      val userEvents        = userEventsService
      val user              = userService
      val callbacks         = callbackHandler
    }
  }

}
