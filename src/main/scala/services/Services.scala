package io.blindnet.privacy
package services

import cats.effect.*
import cats.effect.std.*
import db.repositories.Repositories

trait Services {
  val privacyRequest: PrivacyRequestService
  val consumerInterface: DataConsumerInterfaceService
}

object Services {
  def make(
      repos: Repositories
  ) = {
    val privacyRequestService    = PrivacyRequestService(repos)
    val consumerInterfaceService = DataConsumerInterfaceService(repos)

    new Services {
      val privacyRequest    = privacyRequestService
      val consumerInterface = consumerInterfaceService
    }
  }

}
