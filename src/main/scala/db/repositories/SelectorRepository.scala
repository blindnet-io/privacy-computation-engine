package io.blindnet.privacy
package db.repositories

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import model.vocabulary.*
import model.vocabulary.general.*
import model.vocabulary.general.*
import model.vocabulary.terms.*
import db.DbUtil

trait SelectorRepository  {
  def getSelectors(appId: String, userIds: List[DataSubject]): IO[List[Selector]]

  def addSelectors(appId: String, selectors: List[Selector]): IO[Unit]

  def deleteSelectors(appId: String, selectorIds: List[String]): IO[Unit]

  def getSelectorPrivacyScope(
      appId: String,
      selectors: List[String]
  ): IO[Map[String, List[PrivacyScopeTriple]]]

  def getLegalBasesForSelector(
      appId: String,
      selectors: List[String]
  ): IO[Map[String, List[LegalBase]]]

  def getRetentionPoliciesForSelector(
      appId: String,
      selectors: NonEmptyList[String]
  ): IO[Map[String, List[RetentionPolicy]]]

}

// TODO: select for users
object SelectorRepository {
  def live(xa: Transactor[IO]): SelectorRepository =
    new SelectorRepository {
      def getSelectors(appId: String, userIds: List[DataSubject]): IO[List[Selector]] =
        fr"select id, name, provenance from selectors where active and appid = $appId::uuid"
          .query[(String, String, String)]
          .to[List]
          .map(_.flatMap {
            case (id, name, provenance) =>
              for {
                p <- Provenance.parse(provenance).toOption
              } yield Selector(id, name, p)
          })
          .transact(xa)

      def addSelectors(appId: String, selectors: List[Selector]): IO[Unit] = ???

      def deleteSelectors(appId: String, selectorIds: List[String]): IO[Unit] = ???

      def getSelectorPrivacyScope(
          appId: String,
          selectors: List[String]
      ): IO[Map[String, List[PrivacyScopeTriple]]] = ???

      def getLegalBasesForSelector(
          appId: String,
          selectors: List[String]
      ): IO[Map[String, List[LegalBase]]] = ???

      def getRetentionPoliciesForSelector(
          appId: String,
          selectors: NonEmptyList[String]
      ): IO[Map[String, List[RetentionPolicy]]] =
        (fr"select s.id, rp.policy, rp.duration, rp.after from retention_policies rp join selectors s on s.id = rp.sid where" ++
          DbUtil.Fragments.inUuid(fr"rp.sid", selectors))
          .query[(String, String, String, String)]
          .to[List]
          .map(_.flatMap {
            case (s, policy, dur, after) =>
              for {
                p <- RetentionPolicyTerms.parse(policy).toOption
                a <- EventTerms.parse(after).toOption
              } yield s -> RetentionPolicy(p, dur, a)
          }.groupBy(_._1).view.mapValues(_.map(_._2)).toMap)
          .transact(xa)

    }

}
