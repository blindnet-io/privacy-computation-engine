package io.blindnet.pce
package priv

import java.time.Instant
import java.util.UUID
import io.blindnet.pce.priv.terms.LegalBaseTerms
import io.blindnet.pce.priv.terms.DataCategory

case class Timeline(
    events: List[TimelineEvent]
) {

  def eligiblePrivacyScope(
      timestamp: Option[Instant] = None,
      regulations: List[Regulation] = List.empty,
      selectors: Set[DataCategory] = Set.empty
  ): PrivacyScope = {

    import TimelineEvent.*
    import terms.EventTerms.*
    import terms.LegalBaseTerms.*

    case class Acc(
        objectScope: PrivacyScope,
        restrictScope: PrivacyScope,
        events: List[TimelineEvent]
    )

    def addEvent(ev: TimelineEvent, acc: Acc) =
      acc.copy(events = ev :: acc.events)

    def removeEvent(id: UUID, acc: Acc) =
      acc.copy(events = acc.events.filterNot {
        case ev: LegalBase if ev.lbId == id    => true
        case ev: ConsentGiven if ev.lbId == id => true
        case _                                 => false
      })

    val withDateFilter = timestamp match {
      case Some(t) => events.filter(_.getTimestamp.isBefore(t))
      case None    => events
    }

    // TODO: O(n^2). optimize
    val compiled =
      withDateFilter
        .foldLeft(Acc(PrivacyScope.empty, PrivacyScope.empty, List.empty))(
          (acc, event) => {
            event match {
              case e @ LegalBase(_, RelationshipStart | ServiceStart, Necessary, _, _) =>
                addEvent(e.copy(scope = e.scope.zoomIn(selectors)), acc)
              case LegalBase(id, RelationshipEnd | ServiceEnd, Necessary, _, _)        =>
                removeEvent(id, acc)

              case e @ LegalBase(_, RelationshipStart | ServiceStart, Contract, _, _) =>
                addEvent(e.copy(scope = e.scope.zoomIn(selectors)), acc)
              case LegalBase(id, RelationshipEnd | ServiceEnd, Contract, _, _)        =>
                removeEvent(id, acc)

              case e @ LegalBase(_, RelationshipStart | ServiceStart, LegitimateInterest, _, _) =>
                addEvent(e.copy(scope = e.scope.zoomIn(selectors)), acc)
              case LegalBase(id, RelationshipEnd | ServiceEnd, LegitimateInterest, _, _)        =>
                removeEvent(id, acc)

              case ev: ConsentGiven => addEvent(ev.copy(scope = ev.scope.zoomIn(selectors)), acc)

              case ev: ConsentRevoked => removeEvent(ev.lbId, acc)

              case Restrict(_, scope) =>
                val newRestrictScope =
                  if (acc.restrictScope.isEmpty) then scope.zoomIn(selectors)
                  else acc.restrictScope intersection scope.zoomIn(selectors)
                acc.copy(
                  restrictScope = newRestrictScope,
                  events = acc.events.map {
                    case ev: ConsentGiven => ev.copy(scope = ev.scope intersection newRestrictScope)
                    case ev               => ev
                  }
                )

              case Object(_, scope) =>
                val newObjectScope = acc.objectScope union scope.zoomIn(selectors)
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

    // TODO: optimize
    def forbiddenScopeRegulation(lb: LegalBaseTerms) =
      regulations
        .flatMap(r => r.prohibitedScope.filter(_._1 == lb).map(_._2))
        .foldLeft(PrivacyScope.empty)(_ union _)

    val scope = compiled.events.foldLeft(PrivacyScope.empty)(
      (acc, cur) =>
        cur match {

          case ev: LegalBase if ev.lb == LegitimateInterest =>
            acc union (ev.scope difference compiled.objectScope intersection compiled.restrictScope
              difference forbiddenScopeRegulation(ev.lb))

          case ev: LegalBase =>
            acc union (ev.getScope difference forbiddenScopeRegulation(ev.lb))

          case ev => acc union cur.getScope
        }
    )

    scope
  }

  // TODO: repeating code
  def activeLegalBases(timestamp: Option[Instant]): List[TimelineEvent] = {
    import TimelineEvent.*
    import terms.EventTerms.*
    import terms.LegalBaseTerms.*

    val withDateFilter = timestamp match {
      case Some(t) => events.filter(_.getTimestamp.isBefore(t))
      case None    => events
    }

    def removeEvent(id: UUID, events: List[TimelineEvent]) =
      events.filterNot {
        case ev: LegalBase if ev.lbId == id    => true
        case ev: ConsentGiven if ev.lbId == id => true
        case _                                 => false
      }

    withDateFilter.foldLeft(List.empty[TimelineEvent])(
      (acc, event) => {
        event match {
          case LegalBase(_, RelationshipStart | ServiceStart, _, _, _)      =>
            event :: acc
          case LegalBase(id, RelationshipEnd | ServiceEnd, Necessary, _, _) =>
            removeEvent(id, acc)
          case ev: ConsentGiven                                             => event :: acc
          case ev: ConsentRevoked => removeEvent(ev.lbId, acc)
          case _                  => acc
        }
      }
    )
  }

}

object Timeline {
  def apply(e: TimelineEvent*) = new Timeline(e.toList)

  val empty = Timeline(List.empty)
}
