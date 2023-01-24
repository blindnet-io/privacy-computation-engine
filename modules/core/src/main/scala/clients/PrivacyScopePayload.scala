package io.blindnet.pce
package clients

import io.blindnet.pce.util.parsing.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import org.http4s.Uri
import org.http4s.circe.*
import io.blindnet.pce.priv.PrivacyScope
import java.util.UUID

case class PrivacyScopePayload(
    subjects: List[String],
    lost: List[PrivacyScopeItem],
    callback: Uri
) derives Encoder.AsObject

case class PrivacyScopeItem(
    id: UUID,
    scope: PrivacyScope
) derives Encoder.AsObject
