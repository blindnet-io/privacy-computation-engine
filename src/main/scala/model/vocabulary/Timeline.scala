package io.blindnet.privacy
package model.vocabulary

import io.blindnet.privacy.model.vocabulary.*
import io.blindnet.privacy.model.vocabulary.terms.*
import io.blindnet.privacy.api.endpoints.messages.privacyrequest.Restriction

case class Timeline(
    events: List[TimelineEvent]
) {

  def eligiblePrivacyScope = {

    import TimelineEvent.*
    import EventTerms.*
    import LegalBaseTerms.*

    case class Acc(
        objectScope: PrivacyScope,
        restrictScope: PrivacyScope,
        events: List[TimelineEvent]
    )

    def addEvent(ev: TimelineEvent, acc: Acc) =
      acc.copy(events = ev :: acc.events)

    def removeEvent(id: String, acc: Acc) =
      acc.copy(events = acc.events.filterNot {
        case ev: LegalBase if ev.lbId == id      => true
        case ev: ConsentRevoked if ev.lbId == id => true
        case _                                   => false
      })

    // TODO: O(n^2). optimize
    val compiled =
      events.foldLeft(Acc(PrivacyScope.empty, PrivacyScope.empty, List.empty))(
        (acc, event) => {
          event match {
            case LegalBase(_, RelationshipStart | ServiceStart, Necessary, _, _) =>
              addEvent(event, acc)
            case LegalBase(id, RelationshipEnd | ServiceEnd, Necessary, _, _)    =>
              removeEvent(id, acc)

            case LegalBase(_, RelationshipStart | ServiceStart, Contract, _, _) =>
              addEvent(event, acc)
            case LegalBase(id, RelationshipEnd | ServiceEnd, Contract, _, _)    =>
              removeEvent(id, acc)

            case LegalBase(_, RelationshipStart | ServiceStart, LegitimateInterest, _, _) =>
              addEvent(event, acc)
            case LegalBase(id, RelationshipEnd | ServiceEnd, LegitimateInterest, _, _)    =>
              removeEvent(id, acc)

            case ev: ConsentGiven => addEvent(ev, acc)

            case ev: ConsentRevoked => removeEvent(ev.lbId, acc)

            case Restrict(_, scope) =>
              val newRestrictScope = acc.restrictScope intersection scope
              acc.copy(
                restrictScope = newRestrictScope,
                events = acc.events.map {
                  case ev: ConsentGiven => ev.copy(scope = ev.scope intersection newRestrictScope)
                  case ev               => ev
                }
              )

            case Object(_, scope) =>
              val newObjectScope = acc.objectScope union scope
              acc.copy(
                objectScope = newObjectScope,
                events = acc.events.map {
                  case ev: ConsentGiven => ev.copy(scope = ev.scope difference newObjectScope)
                  case ev               => ev
                }
              )

            case _ => acc
          }
        }
      )

    val scope = compiled.events.foldLeft(PrivacyScope.empty)(
      (acc, cur) =>
        cur match {

          case ev: LegalBase if ev.lb == LegitimateInterest =>
            acc union ((ev.scope difference compiled.objectScope) intersection compiled.restrictScope)

          case ev => acc union cur.getScope
        }
    )

    scope
  }

}