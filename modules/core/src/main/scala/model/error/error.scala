package io.blindnet.pce
package model.error

import scala.util.control.NoStackTrace

import cats.data.NonEmptyList
import cats.effect.IO
import io.circe.Json

case class ValidationException(errors: NonEmptyList[String]) extends NoStackTrace

object ValidationException {
  def apply(error: String) = new ValidationException(NonEmptyList.one(error))
}

case class BadRequestException(message: String)       extends Exception(message)
case class ForbiddenException(message: String = null) extends Exception(message)
case class NotFoundException(message: String = null)  extends Exception(message)
case class AuthException(message: String = null)      extends Exception(message)
case class InternalException(message: String = null)  extends Exception(message)
case class MigrationError(message: String)            extends Error(message)

extension (e: Exception) {
  def raise = IO.raiseError(e)
}
