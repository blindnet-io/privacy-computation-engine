package io.blindnet.pce
package priv

import terms.*
import java.util.UUID

case class Provenance(
    id: UUID,
    provenance: ProvenanceTerms,
    system: String
)
