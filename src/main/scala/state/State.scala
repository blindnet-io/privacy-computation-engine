package io.blindnet.privacy
package state

import cats.effect.*
import cats.effect.std.*

case class State(
    pendingRequests: Queue[IO, String]
)

object State {
  def make() =
    for {
      pendingRequests <- Queue.unbounded[IO, String]
    } yield State(pendingRequests)

}
