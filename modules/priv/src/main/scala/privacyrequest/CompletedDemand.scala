package io.blindnet.pce
package priv
package privacyrequest

import java.util.UUID

import cats.data.*
import cats.implicits.*
import terms.*
import java.time.Instant

case class CompletedDemand(
    id: UUID,
    action: Action,
    dataSubject: Option[DataSubject],
    requestDate: Instant,
    responseDate: Instant,
    status: Status
)
