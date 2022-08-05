package io.blindnet.privacy
package model.vocabulary

import io.blindnet.privacy.model.vocabulary.*
import io.blindnet.privacy.model.vocabulary.terms.*
import io.blindnet.privacy.api.endpoints.messages.privacyrequest.Restriction

case class Timeline(
    events: List[TimelineEvent]
) {
  def eligiblePrivScope = {

    case class Acc(
        objectScope: PrivacyScope,
        restrictScope: PrivacyScope,
        events: List[TimelineEvent]
    )

    // TODO: O(n^2). optimize
    val compiled =
      events.foldLeft(Acc(PrivacyScope.empty, PrivacyScope.empty, List.empty))(
        (acc, event) => {
          event match {
            // TODO: split
            case ev: TimelineEvent.LegalBase
                if ev.lb == LegalBaseTerms.Necessary &&
                  (ev.eType == EventTerms.RelationshipStart || ev.eType == EventTerms.ServiceStart) =>
              acc.copy(events = ev :: acc.events)

            case ev: TimelineEvent.LegalBase
                if ev.lb == LegalBaseTerms.Necessary &&
                  (ev.eType == EventTerms.RelationshipEnd || ev.eType == EventTerms.ServiceEnd) =>
              acc.copy(events = acc.events.filterNot {
                case e: TimelineEvent.LegalBase if e.lbId == ev.lbId => true
                case _                                               => false
              })

            case ev: TimelineEvent.LegalBase
                if ev.lb == LegalBaseTerms.Contract &&
                  (ev.eType == EventTerms.RelationshipStart || ev.eType == EventTerms.ServiceStart) =>
              acc.copy(events = ev :: acc.events)

            case ev: TimelineEvent.LegalBase
                if ev.lb == LegalBaseTerms.Contract &&
                  (ev.eType == EventTerms.RelationshipEnd || ev.eType == EventTerms.ServiceEnd) =>
              acc.copy(events = acc.events.filterNot {
                case e: TimelineEvent.LegalBase if e.lbId == ev.lbId => true
                case _                                               => false
              })

            case ev: TimelineEvent.LegalBase
                if ev.lb == LegalBaseTerms.LegitimateInterest &&
                  (ev.eType == EventTerms.RelationshipStart || ev.eType == EventTerms.ServiceStart) =>
              acc.copy(events = ev :: acc.events)

            case ev: TimelineEvent.LegalBase
                if ev.lb == LegalBaseTerms.LegitimateInterest &&
                  (ev.eType == EventTerms.RelationshipEnd || ev.eType == EventTerms.ServiceEnd) =>
              acc.copy(events = acc.events.filterNot {
                case e: TimelineEvent.LegalBase if e.lbId == ev.lbId => true
                case _                                               => false
              })

            case ev: TimelineEvent.ConsentGiven =>
              acc.copy(events = ev :: acc.events)

            case ev: TimelineEvent.ConsentRevoked =>
              acc.copy(events = acc.events.filterNot {
                case e: TimelineEvent.ConsentGiven if e.lbId == ev.lbId => true
                case _                                                  => false
              })

            case TimelineEvent.Restrict(_, scope) =>
              val newRestrictScope = acc.restrictScope intersection scope
              acc.copy(
                restrictScope = newRestrictScope,
                events = acc.events.map {
                  case ev: TimelineEvent.ConsentGiven =>
                    ev.copy(scope = ev.scope intersection newRestrictScope)
                  case ev                             => ev
                }
              )

            case TimelineEvent.Object(_, scope) =>
              val newObjectScope = acc.objectScope union scope
              acc.copy(
                objectScope = newObjectScope,
                events = acc.events.map {
                  case ev: TimelineEvent.ConsentGiven =>
                    ev.copy(scope = ev.scope difference newObjectScope)
                  case ev                             => ev
                }
              )

            case _ => acc
          }
        }
      )

    val scope = compiled.events.foldLeft(PrivacyScope.empty)(
      (acc, cur) =>
        cur match {

          case ev: TimelineEvent.LegalBase if ev.lb == LegalBaseTerms.LegitimateInterest =>
            acc union ((ev.scope difference compiled.objectScope) intersection compiled.restrictScope)

          case ev => acc union cur.getScope
        }
    )

    scope
  }

}
