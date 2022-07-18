package io.blindnet.privacy
package model.error

import scala.util.control.NoStackTrace
import cats.data.NonEmptyList

case class ValidationError(errors: NonEmptyList[String]) extends NoStackTrace

object ValidationError {
  def apply(error: String) = new ValidationError(NonEmptyList.one(error))
}

case class MigrationError(message: String) extends Error(message)
