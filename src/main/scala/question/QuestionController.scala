package question

import cats.effect.IO
import sttp.tapir.server.ServerEndpoint
import infrastructure.auth.AuthenticationService
import user.TeacherId

class QuestionController(questionService: QuestionService, authenticationService: AuthenticationService):
  import authenticationService.*

  def getExamQuestions = QuestionEndpoints.getExamQuestionsEndpoint.authenticate.serverLogic { user => examId =>
    questionService.getQuestionsForExam(user.id, examId)
  }

  def addQuestion = QuestionEndpoints.addQuestionEndpoint.authenticate.serverLogic { user => (examId, form) =>
    questionService.addQuestion(form, examId, TeacherId(user.id))
  }

  def deleteQuestion = QuestionEndpoints.deleteQuestionEndpoint.authenticate.serverLogic { user => (examId, questionId) =>
    questionService.deleteQuestion(questionId, examId, TeacherId(user.id))
  }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(getExamQuestions, addQuestion, deleteQuestion)
