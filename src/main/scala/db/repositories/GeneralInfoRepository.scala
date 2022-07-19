package io.blindnet.privacy
package db.repositories

import cats.effect.IO
import model.vocabulary.*
import model.vocabulary.general.*
import model.vocabulary.general.*
import model.vocabulary.terms.*

trait GeneralInfoRepository[F[_]] {
  def getGeneralInfo(appId: String): F[GeneralInformation]

  def known(appId: String, userIds: List[DataSubject]): F[Boolean]

  def getPrivacyPolicy(appId: String): F[String]
}
