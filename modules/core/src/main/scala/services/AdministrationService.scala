package io.blindnet.pce
package services

import cats.effect.*
import cats.implicits.*
import io.blindnet.pce.util.extension.*
import api.endpoints.messages.administration.*
import db.repositories.*

class AdministrationService(
    repos: Repositories
) {

  def createApp(x: Unit)(req: CreateApplication) =
    repos.app.create(req.appId)

}
