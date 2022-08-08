package io.blindnet.privacy
package util

import cats.*
import cats.data.*
import cats.implicits.*
import io.blindnet.privacy.model.error.NotFoundException

object extension {
  extension [M[_], A](m: M[Option[A]])
    def toOptionT =
      OptionT(m)

  extension [M[_]: Functor, G[_], A](m: M[G[A]])
    def toOptionT =
      OptionT(m.map(g => Option(g)))

  extension [M[_], A](m: M[Option[A]])(using M: MonadError[M, Throwable])
    def orNotFound(msg: String) = m.flatMap {
      case None    => M.raiseError(new NotFoundException(msg))
      case Some(x) => M.pure(x)
    }

  extension [M[_]](m: M[Boolean])(using M: MonadError[M, Throwable])
    def emptyNotFound(msg: String) = m.flatMap {
      case false => M.raiseError(new NotFoundException(msg))
      case true  => M.pure(())
    }

}
