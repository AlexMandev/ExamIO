package client

import cats.effect.IO
import sttp.client4.Backend
import sttp.model.Uri
import sttp.tapir.{Endpoint, PublicEndpoint}
import sttp.tapir.client.sttp4.SttpClientInterpreter

class ApiClient(backend: Backend[IO], apiBaseUrl: Uri):
  private val interpreter = SttpClientInterpreter()

  def request[I, E, O](endpoint: PublicEndpoint[I, E, O, Any]): I => IO[Either[E, O]] =
    interpreter.toClientThrowDecodeFailures(endpoint, Some(apiBaseUrl), backend)

  def secureRequest[S, I, E, O](endpoint: Endpoint[S, I, E, O, Any]): S => I => IO[Either[E, O]] =
    interpreter.toSecureClientThrowDecodeFailures(endpoint, Some(apiBaseUrl), backend)
