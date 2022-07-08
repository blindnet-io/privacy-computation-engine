package io.blindnet.privacy
package model.vocabulary.terms

enum Target(term: String, parent: Option[Target] = None) {
  case Organization extends Target("ORGANIZATION")
  case System       extends Target("SYSTEM")
  case Partners     extends Target("PARTNERS")
  case PDownward    extends Target("PARTNERS.DOWNWARD", Some(Partners))
  case PUpward      extends Target("PARTNERS.UPWARD", Some(Partners))

  def allSubCategories(): List[Target] = {
    val children = Target.values.filter(_.isChildOf(this)).toList
    this +: children.flatMap(_.allSubCategories())
  }

  def isChildOf(a: Target) =
    parent.exists(_ == a)

  def isTerm(str: String) = term == str
}

object Target {
  def parse(str: String): Option[Target] =
    Target.values.find(a => a.isTerm(str))

}
