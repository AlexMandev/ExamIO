package answers

import cats.effect.IO
import sttp.tapir.server.ServerEndpoint

import infrastructure.auth.AuthenticationService

import user.StudentId
import grading.GradingService
import user.TeacherId

class AnswerController(answerService: AnswerService, gradingService: GradingService, authenticationService: AuthenticationService):
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

  def gradeAnswer = AnswerEndpoints.gradeAnswerEndpoint.authenticate.serverLogic {
    user => (examId, submissionId, questionId, gradeAnswerForm) =>
      gradingService.gradeShortAnswer(TeacherId(user.id), examId, submissionId, questionId, gradeAnswerForm)
  }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(getAnswer, addAnswer, clearAnswer)
