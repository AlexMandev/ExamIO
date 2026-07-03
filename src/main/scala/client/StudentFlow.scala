package client

import cats.effect.IO

class StudentFlow(client: ExamIOApiClient, token: String):
  def run: IO[Unit] = ???
