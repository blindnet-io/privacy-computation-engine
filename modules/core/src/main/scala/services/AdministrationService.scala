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

  def createApp(x: Unit)(req: CreateApplicationPayload) =
    for {
      app <- repos.app.get(req.appId)
      _   <- app match {
        case None => repos.app.create(req.appId)
        case _    => s"Application ${req.appId} exists".failBadRequest
      }
    } yield ()

}
