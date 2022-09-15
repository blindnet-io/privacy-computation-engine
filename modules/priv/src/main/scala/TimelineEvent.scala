package io.blindnet.pce
package priv

import java.time.Instant
import java.util.UUID

import terms.*

enum TimelineEvent(timestamp: Instant) {

  case LegalBase(
      lbId: UUID,
      eType: EventTerms,
      lb: LegalBaseTerms,
      timestamp: Instant,
      scope: PrivacyScope
  ) extends TimelineEvent(timestamp)

  case ConsentGiven(
      lbId: UUID,
      timestamp: Instant,
      scope: PrivacyScope
  ) extends TimelineEvent(timestamp)

  case ConsentRevoked(
      lbId: UUID,
      timestamp: Instant
  ) extends TimelineEvent(timestamp)

  case Restrict(
      timestamp: Instant,
      scope: PrivacyScope
  ) extends TimelineEvent(timestamp)

  case Object(
      timestamp: Instant,
      scope: PrivacyScope
  ) extends TimelineEvent(timestamp)

  val getTimestamp = this.timestamp

  def getLbId = this match {
    case lb: LegalBase    => Some(lb.lbId)
    case cg: ConsentGiven => Some(cg.lbId)
    case _                => None
  }

  val getScope = this match {
    case lb: LegalBase      => lb.scope
    case c: ConsentGiven    => c.scope
    case cr: ConsentRevoked => PrivacyScope.empty
    case r: Restrict        => r.scope
    case o: Object          => o.scope
  }

  def withGranularScope(ctx: PSContext) = this match {
    case lb: LegalBase      => lb.copy(scope = lb.scope.zoomIn(ctx))
    case c: ConsentGiven    => c.copy(scope = c.scope.zoomIn(ctx))
    case cr: ConsentRevoked => cr
    case r: Restrict        => r.copy(scope = r.scope.zoomIn(ctx))
    case o: Object          => o.copy(scope = o.scope.zoomIn(ctx))
  }

}
