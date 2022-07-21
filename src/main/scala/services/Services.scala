package io.blindnet.privacy
package services

import cats.effect.*
import db.repositories.Repositories

trait Services {
  val privacyRequest: PrivacyRequestService
}

object Services {
  def make(repos: Repositories) = {
    val privacyRequestService =
      PrivacyRequestService(repos)

    new Services {
      val privacyRequest = privacyRequestService
    }
  }

}
