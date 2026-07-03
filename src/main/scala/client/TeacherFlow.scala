package client

import cats.effect.IO

class TeacherFlow(client: ExamIOApiClient, token: String):
  def run: IO[Unit] = ???
