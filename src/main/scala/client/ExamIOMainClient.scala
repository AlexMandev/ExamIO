package client

import cats.syntax.all.*
import cats.effect.{IO, IOApp}
import sttp.client4.httpclient.cats.HttpClientCatsBackend
import sttp.client4.UriContext

object ExamIOMainClient extends IOApp.Simple:
  private val catsBackend = HttpClientCatsBackend.resource[IO]()

  def run: IO[Unit] =
    catsBackend.use { backend =>
      val apiClient = ApiClient(backend, uri"http://localhost:3000")
      val examIOApiClient = ExamIOApiClient(apiClient)

      CommonFlow.startApp(examIOApiClient)
    }
