package io.blindnet.pce
package priv

import terms.*
import java.time.Instant
import java.util.UUID

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

  val getScope = this match {
    case lb: LegalBase      => lb.scope
    case c: ConsentGiven    => c.scope
    case cr: ConsentRevoked => PrivacyScope.empty
    case r: Restrict        => r.scope
    case o: Object          => o.scope
  }

}
