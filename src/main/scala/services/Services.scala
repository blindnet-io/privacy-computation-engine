package io.blindnet.privacy
package services

import cats.effect.*
import db.repositories.Repositories

trait Services {
  val privacyRequest: PrivacyRequestService
}

object Services {
  def make(repos: Repositories) = {
    val privacyRequest =
      PrivacyRequestService(repos.generalInfo, repos.privacyScope, repos.legalBase)

    new Services {
      val privacyRequest = privacyRequest
    }
  }

}
