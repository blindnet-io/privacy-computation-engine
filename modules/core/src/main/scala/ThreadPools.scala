package io.blindnet.pce

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import cats.effect.*

case class Pools(
    cpu: ExecutionContext
)

object Pools {

  val cpu: Resource[IO, ExecutionContext] = {
    val size    = Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
    val acquire = IO(Executors.newFixedThreadPool(size))
    Resource.make(acquire)(p => IO(p.shutdown())).map(ExecutionContext.fromExecutor)
  }

}
