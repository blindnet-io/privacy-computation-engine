package io.blindnet.privacy
package util

import cats.*
import cats.data.*
import cats.implicits.*

object extension {
  extension [M[_], A](m: M[Option[A]]) {
    def toOptionT = OptionT(m)
  }

  extension [M[_]: Functor, G[_], A](m: M[G[A]]) {
    def toOptionT = OptionT(m.map(g => Option(g)))
  }

}
