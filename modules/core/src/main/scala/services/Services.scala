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
  val bridge: BridgeService
  val configuration: ConfigurationService
  val administration: AdministrationService
  val userEvents: UserEventsService
  val user: UserService
  val callbacks: CallbackHandler
}

object Services {
  def make(
      repos: Repositories,
      conf: Config
  ) = {
    lazy val privacyRequestService = PrivacyRequestService(repos)
    lazy val bridgeService         = BridgeService(repos)
    lazy val configurationService  = ConfigurationService(repos)
    lazy val administrationService = AdministrationService(repos)
    lazy val userEventsService     = UserEventsService(repos)
    lazy val userService           = UserService(repos)
    lazy val callbackHandler       = CallbackHandler(repos)

    new Services {
      val privacyRequest = privacyRequestService
      val bridge         = bridgeService
      val configuration  = configurationService
      val administration = administrationService
      val userEvents     = userEventsService
      val user           = userService
      val callbacks      = callbackHandler
    }
  }

}
