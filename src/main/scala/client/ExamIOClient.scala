package client

import cats.effect.{IO, IOApp}
import sttp.client4.httpclient.cats.HttpClientCatsBackend
import sttp.model.Uri

object ExamIOClient extends IOApp.Simple:
  private val catsBackend = HttpClientCatsBackend.resource[IO]()

  def run: IO[Unit] =
    catsBackend.use { backend =>
      val apiClient = ApiClient(backend, Uri("http://localhost:3000"))

      ???
    }
