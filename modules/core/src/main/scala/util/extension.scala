package io.blindnet.pce
package util

import cats.*
import cats.data.*
import cats.implicits.*
import io.blindnet.pce.model.error.*
import cats.effect.IO

object extension {
  extension [M[_], A](m: M[Option[A]])
    def toOptionT =
      OptionT(m)

  extension [M[_]: Functor, G[_], A](m: M[G[A]])
    def toOptionT =
      OptionT(m.map(g => Option(g)))

  extension [M[_]: MonadThrow, A](m: M[Option[A]]) {

    def orNotFound(msg: String): M[A] = m.flatMap {
      case None    => NotFoundException(msg).raiseError
      case Some(x) => x.pure
    }

    def orBadRequest(msg: String): M[A] = m.flatMap {
      case None    => BadRequestException(msg).raiseError
      case Some(x) => x.pure
    }

    def orFail(msg: String): M[A] = m.flatMap {
      case None    => InternalException(msg).raiseError
      case Some(x) => x.pure
    }

  }

  extension [M[_]: MonadThrow](m: M[Boolean]) {
    def onFalseNotFound(msg: String) = m.flatMap {
      case false => NotFoundException(msg).raiseError
      case true  => ().pure
    }

    def onFalseBadRequest(msg: String) = m.flatMap {
      case false => BadRequestException(msg).raiseError
      case true  => ().pure
    }

  }

  extension [A](o: Option[A])
    def orBadRequest(msg: String): IO[A] = o match {
      case None    => BadRequestException(msg).raiseError
      case Some(x) => IO(x)
    }

  extension (b: Boolean) {
    def onFalseNotFound(msg: String) =
      if b then IO.unit else NotFoundException(msg).raise

    def onFalseBadRequest(msg: String) =
      if b then IO.unit else BadRequestException(msg).raise

  }

  extension (s: String) {
    def failBadRequest = BadRequestException(s).raise
    def failNotFound   = NotFoundException(s).raise
  }

}
