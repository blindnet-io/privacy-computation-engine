package io.blindnet.pce
package db.repositories

import java.util.UUID

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import model.PCEApp
import io.blindnet.pce.model.DemandResolutionStrategy

trait AppRepository {
  def get(id: UUID): IO[Option[PCEApp]]

  def updateReslutionStrategy(id: UUID, rs: DemandResolutionStrategy): IO[Unit]
}

object AppRepository {
  def live(xa: Transactor[IO]): AppRepository =
    new AppRepository {

      def get(id: UUID): IO[Option[PCEApp]] =
        sql"""
          select a.id, d.active, d.uri, d.token, arc.auto_transparency, arc.auto_access, arc.auto_delete,
            arc.auto_revoke_consent, arc.auto_object, arc.auto_restrict
          from apps a
            join dac d on d.appid = a.id
            join automatic_responses_config arc on arc.appid = a.id
          where a.id = $id
        """
          .query[PCEApp]
          .option
          .transact(xa)

      def updateReslutionStrategy(id: UUID, rs: DemandResolutionStrategy): IO[Unit] =
        sql"""
          update automatic_responses_config
          set auto_transparency = ${rs.isAutoTransparency},
              auto_access = ${rs.isAutoAccess},
              auto_delete = ${rs.isAutoDelete},
              auto_revoke_consent = ${rs.isAutoRevokeConsent},
              auto_object = ${rs.isAutoObject},
              auto_restrict = ${rs.isAutoRestrict}
          where appid = $id
        """.update.run.transact(xa).void

    }

}
