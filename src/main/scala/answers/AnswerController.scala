package answer

import cats.effect.IO
import sttp.tapir.server.ServerEndpoint

import infrastructure.auth.AuthenticationService

class AnswerController(answerService: AnswerService, authenticationService: AuthenticationService):
  import authenticationService.*

  def getAnswer = AnswerEndpoint.getAnswerEndpoint.authenticate.serverLogic {
    user => (_, submissionId, questionId) => answerService.getAnswer(questionId, submissionId)
  }

  def addAnswer = AnswerEndpoint.addAnswerEndpoint.authenticate.serverLogic {
    user => (_, _, questionId, answerForm) =>
      answerService.addAnswer(answerForm, questionId)
  }

  def clearAnswer = AnswerEndpoint.clearAnswerEndpoint.authenticate.serverLogic {
    user => (_, submissionId, questionId) => answerService.clearAnswer(questionId, submissionId)
  }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(getAnswer, addAnswer, clearAnswer)
