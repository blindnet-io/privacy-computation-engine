package io.blindnet.privacy
package model.vocabulary.terms

enum EventTerms {
  case CaptureDate
  case RelationshipStart
  case RelationshipEnd
  case ServiceStart
  case ServiceEnd
}

object EventTerms {
  def parse(s: String): Option[EventTerms] =
    s match {
      case "CAPTURE-DATE"       => Some(EventTerms.CaptureDate)
      case "RELATIONSHIP-END"   => Some(EventTerms.RelationshipStart)
      case "RELATIONSHIP-START" => Some(EventTerms.RelationshipEnd)
      case "SERVICE-END"        => Some(EventTerms.ServiceStart)
      case "SERVICE-START"      => Some(EventTerms.ServiceEnd)
      case _                    => None
    }

}
