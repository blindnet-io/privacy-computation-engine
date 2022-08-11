package io.blindnet.pce
package util

import cats.*
import cats.data.*
import cats.implicits.*
import io.blindnet.pce.model.error.*

object extension {
  extension [M[_], A](m: M[Option[A]])
    def toOptionT =
      OptionT(m)

  extension [M[_]: Functor, G[_], A](m: M[G[A]])
    def toOptionT =
      OptionT(m.map(g => Option(g)))

  extension [M[_]: MonadThrow, A](m: M[Option[A]]) {

    def orNotFound(msg: String): M[A] = m.flatMap {
      case None    => new NotFoundException(msg).raiseError
      case Some(x) => x.pure
    }

    def orFail(msg: String): M[A] = m.flatMap {
      case None    => new InternalException(msg).raiseError
      case Some(x) => x.pure
    }

  }

  extension [M[_]: MonadThrow](m: M[Boolean])
    def emptyNotFound(msg: String): M[Unit] = m.flatMap {
      case false => new NotFoundException(msg).raiseError
      case true  => ().pure
    }

}
