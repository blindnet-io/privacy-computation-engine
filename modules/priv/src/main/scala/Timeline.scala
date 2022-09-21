package io.blindnet.pce
package priv

import java.time.Instant
import java.util.UUID

import io.blindnet.pce.priv.terms.{ DataCategory, LegalBaseTerms }

case class Timeline(
    events: List[TimelineEvent]
) {

  import TimelineEvent.*
  import terms.EventTerms.*
  import terms.LegalBaseTerms.*

  def filteredByTime(t: Instant) = events.filter(_.getTimestamp.isBefore(t))

  def compiledEvents(
      timestamp: Option[Instant] = None,
      regulations: List[Regulation] = List.empty
  ): List[TimelineEvent] = {

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

    // TODO: O(n^2). optimize
    val compiled =
      timestamp
        .map(filteredByTime)
        .getOrElse(events)
        .foldLeft(Acc(PrivacyScope.empty, PrivacyScope.empty, List.empty))(
          (acc, event) => {
            event match {
              case ev @ LegalBase(_, RelationshipStart | ServiceStart, _, _, _) =>
                addEvent(ev.copy(scope = ev.scope), acc)
              case LegalBase(id, RelationshipEnd | ServiceEnd, _, _, _)         =>
                removeEvent(id, acc)

              case ev: ConsentGiven =>
                addEvent(ev.copy(scope = ev.scope), acc)

              case ev: ConsentRevoked => removeEvent(ev.lbId, acc)

              case Restrict(_, scope) =>
                val newRestrictScope =
                  if (acc.restrictScope.isEmpty) then scope
                  else acc.restrictScope intersection scope
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

    // TODO: optimize
    def forbiddenScopeRegulation(lb: LegalBaseTerms) =
      regulations
        .flatMap(r => r.prohibitedScope.filter(_._1 == lb).map(_._2))
        .foldLeft(PrivacyScope.empty)(_ union _)

    val finalEvents = compiled.events.foldLeft(List.empty[TimelineEvent])(
      (acc, cur) =>
        cur match {

          case ev: LegalBase if ev.lb == LegitimateInterest =>
            ev.copy(scope =
              ev.scope difference
                compiled.objectScope intersection
                compiled.restrictScope difference
                forbiddenScopeRegulation(ev.lb)
            ) :: acc

          case ev: LegalBase =>
            ev.copy(scope = ev.scope difference forbiddenScopeRegulation(ev.lb)) :: acc

          case ev => cur :: acc
        }
    )

    finalEvents
  }

  def eligiblePrivacyScope(
      timestamp: Option[Instant] = None,
      regulations: List[Regulation] = List.empty
  ): PrivacyScope =
    Timeline.eligiblePrivacyScope(compiledEvents(timestamp, regulations))

  def getConsentGivenEvents(timestamp: Option[Instant] = None): List[ConsentGiven] =
    compiledEvents(timestamp).flatMap {
      case e: ConsentGiven => Some(e)
      case _               => None
    }

}

object Timeline {
  private def mk(evs: List[TimelineEvent], ctx: PSContext) =
    new Timeline(evs.map(ev => ev.withGranularScope(ctx)))

  def create(evs: List[TimelineEvent])(ctx: PSContext) = mk(evs, ctx)

  def create(evs: TimelineEvent*)(ctx: PSContext) = mk(evs.toList, ctx)

  def eligiblePrivacyScope(events: List[TimelineEvent]): PrivacyScope =
    events.foldLeft(PrivacyScope.empty)(_ union _.getScope)

  val empty = Timeline(List.empty)
}
