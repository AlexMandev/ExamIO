package infrastructure

import sttp.tapir.*

object ExamIOEndpoints:
  private val baseEndpoint = endpoint

  val apiBaseEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] = endpoint.in("api").in("v1")
