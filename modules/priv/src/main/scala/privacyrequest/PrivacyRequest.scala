package io.blindnet.pce
package priv
package privacyrequest

import java.time.Instant
import java.util.UUID
import cats.data.*
import cats.implicits.*
import sttp.tapir.{Codec, DecodeResult}
import sttp.tapir.Codec.PlainCodec
import terms.*

opaque type RequestId = UUID
object RequestId:
  def apply(uuid: UUID): RequestId          = uuid
  extension (id: RequestId) def value: UUID = id
  given Ordering[RequestId]                 = Ordering.fromLessThan(_ < _)
  given PlainCodec[RequestId]               = Codec.uuid.mapDecode(DecodeResult.Value(_))(_.value)


case class PrivacyRequest(
    id: RequestId,
    appId: UUID,
    timestamp: Instant,
    target: Target,
    email: Option[String],
    dataSubject: Option[DataSubject],
    providedDsIds: List[String],
    demands: List[Demand]
)

object PrivacyRequest {
  def validateDemands(pr: PrivacyRequest): (List[(NonEmptyList[String], Demand)], List[Demand]) = {
    pr.demands.foldLeft((List.empty[(NonEmptyList[String], Demand)], List.empty[Demand]))(
      (acc, cur) => {
        Demand
          .validate(cur)
          .fold(
            errs => ((errs, cur) :: acc._1, acc._2),
            d => (acc._1, cur :: acc._2)
          )
      }
    )
  }

}
