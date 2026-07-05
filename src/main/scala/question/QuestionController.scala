package question

import cats.effect.IO
import sttp.tapir.server.ServerEndpoint
import infrastructure.auth.AuthenticationService
import user.TeacherId
import user.StudentId

class QuestionController(questionService: QuestionService, authenticationService: AuthenticationService):
  import authenticationService.*

  def getExamQuestions = QuestionEndpoints.getExamQuestionsEndpoint.authenticate.serverLogic { user => examId =>
    questionService.getQuestionsForExam(user.id, examId)
  }

  def getStudentQuestions = QuestionEndpoints.getStudentQuestionsEndpoint.authenticate.serverLogic(
    user => (examId, submissionId) => questionService.getStudentQuestions(StudentId(user.id), examId, submissionId)
  )

  def addQuestion = QuestionEndpoints.addQuestionEndpoint.authenticate.serverLogic { user => (examId, form) =>
    questionService.addQuestion(form, examId, TeacherId(user.id))
  }

  def deleteQuestion = QuestionEndpoints.deleteQuestionEndpoint.authenticate.serverLogic {
    user => (examId, questionId) =>
      questionService.deleteQuestion(questionId, examId, TeacherId(user.id))
  }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(getExamQuestions, getStudentQuestions, addQuestion, deleteQuestion)
