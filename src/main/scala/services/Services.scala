package io.blindnet.privacy
package services

import cats.effect.*
import cats.effect.std.*
import db.repositories.Repositories
import state.State

trait Services {
  val privacyRequest: PrivacyRequestService
}

object Services {
  def make(
      repos: Repositories,
      state: State
  ) = {
    val privacyRequestService = PrivacyRequestService(repos, state)

    new Services {
      val privacyRequest = privacyRequestService
    }
  }

}
