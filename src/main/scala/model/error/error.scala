package io.blindnet.privacy
package model.error

import scala.util.control.NoStackTrace

import cats.data.NonEmptyList

case class ValidationException(errors: NonEmptyList[String]) extends NoStackTrace

object ValidationException {
  def apply(error: String) = new ValidationException(NonEmptyList.one(error))
}

case class MigrationError(message: String) extends Error(message)
