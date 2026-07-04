package client.utils

import cats.effect.IO
import cats.syntax.all.*

object OptionUtils:
  extension (optA: Option[String])
    def printLn: IO[Unit] =
      optA.fold(().pure[IO])(s => IO.println(s + "\n"))
