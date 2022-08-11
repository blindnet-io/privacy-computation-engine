package io.blindnet.pce
package services

import cats.effect.*
import cats.effect.std.*
import org.http4s.client.Client
import db.repositories.Repositories
import services.storage.StorageInterface
import config.Config
import io.blindnet.pce.services.*

trait Services {
  val privacyRequest: PrivacyRequestService
  val consumerInterface: DataConsumerInterfaceService
  val callbacks: CallbackService
  val storage: StorageInterface
}

object Services {
  def make(
      repos: Repositories,
      httpClient: Client[IO],
      conf: Config
  ) = {
    lazy val privacyRequestService    = PrivacyRequestService(repos)
    lazy val consumerInterfaceService = DataConsumerInterfaceService(repos, storageInterface)
    lazy val callbackService          = CallbackService(repos)
    lazy val storageInterface         = StorageInterface.live(httpClient, conf)

    new Services {
      val privacyRequest    = privacyRequestService
      val consumerInterface = consumerInterfaceService
      val callbacks         = callbackService
      val storage           = storageInterface
    }
  }

}
