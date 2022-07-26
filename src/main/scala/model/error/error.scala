package io.blindnet.privacy
package model.error

import scala.util.control.NoStackTrace

import cats.data.NonEmptyList

case class ValidationException(errors: NonEmptyList[String]) extends NoStackTrace

object ValidationException {
  def apply(error: String) = new ValidationException(NonEmptyList.one(error))
}

class BadRequestException(message: String)       extends Exception(message)
class ForbiddenException(message: String = null) extends Exception(message)
class NotFoundException(message: String = null)  extends Exception(message)
class AuthException(message: String = null)      extends Exception(message)

case class MigrationError(message: String) extends Error(message)
