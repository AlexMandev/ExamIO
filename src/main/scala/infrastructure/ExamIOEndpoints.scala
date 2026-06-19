package infrastructure

import cats.effect.IO
import cats.syntax.all.*
import sttp.tapir.*

object ExamIOEndpoints:
  private val baseEndpoint = endpoint

  val apiBaseEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] = endpoint.in("api").in("v1")

  val helloEndpoint = apiBaseEndpoint
    .in("say-hello")
    .out(stringBody)
    .get
    .serverLogicSuccess(_ => IO.pure("Hello from ExamIO"))

  val goodbyeEndpoint =
    apiBaseEndpoint.in("say-goodbye").out(stringBody).get.serverLogicSuccess(_ => IO.pure("Goodbye from ExamIO"))

  val exampleEndpoints = List(helloEndpoint, goodbyeEndpoint)
