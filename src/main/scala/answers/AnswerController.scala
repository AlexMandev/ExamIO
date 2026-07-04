package answers

import cats.effect.IO
import sttp.tapir.server.ServerEndpoint

import infrastructure.auth.AuthenticationService

import user.StudentId

class AnswerController(answerService: AnswerService, authenticationService: AuthenticationService):
  import authenticationService.*

  def getAnswer = AnswerEndpoints.getAnswerEndpoint.authenticate
    .serverLogic { user => (examId, submissionId, questionId) =>
      answerService.getAnswer(user.id, questionId, submissionId, examId)
    }

  def addAnswer = AnswerEndpoints.addAnswerEndpoint.authenticate.serverLogic {
    user => (examId, submissionId, questionId, answerForm) =>
      answerService.addAnswer(StudentId(user.id), answerForm, questionId, submissionId, examId)
  }

  def clearAnswer = AnswerEndpoints.clearAnswerEndpoint.authenticate.serverLogic {
    user => (examId, submissionId, questionId) =>
      answerService.clearAnswer(StudentId(user.id), questionId, submissionId, examId)
  }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(getAnswer, addAnswer, clearAnswer)
