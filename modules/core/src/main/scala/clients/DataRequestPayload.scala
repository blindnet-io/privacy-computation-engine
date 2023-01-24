package io.blindnet.pce
package clients

import java.time.Instant

import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import org.http4s.Uri
import org.http4s.circe.*

case class DataQueryPayload(
    selectors: List[String], // empty = everything
    subjects: List[String],
    provenance: Option[String],
    target: Option[String],
    after: Option[Instant],
    until: Option[Instant]
) derives Encoder.AsObject

case class DataRequestPayload(
    query: DataQueryPayload,
    callback: Uri
) derives Encoder.AsObject
