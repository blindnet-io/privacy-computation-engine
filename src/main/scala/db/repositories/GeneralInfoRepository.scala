package io.blindnet.privacy
package db.repositories

import cats.effect.IO
import doobie.util.transactor.Transactor
import model.vocabulary.*
import model.vocabulary.general.*
import model.vocabulary.general.*
import model.vocabulary.terms.*

trait GeneralInfoRepository {
  def getGeneralInfo(appId: String): IO[GeneralInformation]

  def known(appId: String, userIds: List[DataSubject]): IO[Boolean]

  def getPrivacyPolicy(appId: String): IO[String]
}

object GeneralInfoRepository {
  def live(xa: Transactor[IO]): GeneralInfoRepository =
    new GeneralInfoRepository {
      def getGeneralInfo(appId: String): IO[GeneralInformation] = ???

      def known(appId: String, userIds: List[DataSubject]): IO[Boolean] = ???

      def getPrivacyPolicy(appId: String): IO[String] = ???
    }

}
