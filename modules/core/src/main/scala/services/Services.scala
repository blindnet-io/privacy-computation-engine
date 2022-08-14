package io.blindnet.pce
package services

import cats.effect.*
import cats.effect.std.*
import org.http4s.client.Client
import db.repositories.Repositories
import config.Config
import io.blindnet.pce.services.*

trait Services {
  val privacyRequest: PrivacyRequestService
  val consumerInterface: DataConsumerInterfaceService
  val customization: CustomizationService
  val callbacks: CallbackService
}

object Services {
  def make(
      repos: Repositories,
      conf: Config
  ) = {
    lazy val privacyRequestService    = PrivacyRequestService(repos)
    lazy val consumerInterfaceService = DataConsumerInterfaceService(repos)
    lazy val customizationService     = CustomizationService(repos)
    lazy val callbackService          = CallbackService(repos)

    new Services {
      val privacyRequest    = privacyRequestService
      val consumerInterface = consumerInterfaceService
      val customization     = customizationService
      val callbacks         = callbackService
    }
  }

}
