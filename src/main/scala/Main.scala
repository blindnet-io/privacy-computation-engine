package io.blindnet.privacy

import cats.effect.*

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    IO(println("Hello World!")).as(ExitCode.Success)

}
