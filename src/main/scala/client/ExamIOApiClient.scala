package client

import cats.effect.IO
import exam.{CloseExamError, Exam, ExamEndpoints, ExamError, ExamForm, ExamFormValidationError, ExamId, OpenExamError}
import infrastructure.auth.AuthenticationError
import question.{AddQuestionError, DeleteQuestionError, Question, QuestionEndpoints, QuestionForm, QuestionId}
import user.{LoginResponse, User, UserEndpoints, UserLoginForm, UserRegistrationError, UserRegistrationForm}

class ExamIOApiClient(client: ApiClient):
  def login(form: UserLoginForm): IO[Either[Unit, LoginResponse]] =
    client.request(UserEndpoints.loginEndpoint)(form)

  def register(form: UserRegistrationForm): IO[Either[UserRegistrationError, User]] =
    client.request(UserEndpoints.registerUserEndpoint)(form)

  def createExam(form: ExamForm, token: String): IO[Either[AuthenticationError | ExamFormValidationError, Exam]] =
    client.secureRequest(ExamEndpoints.createExamEndpoint)(token)(form)

  def getOwnExams(token: String): IO[Either[AuthenticationError, List[Exam]]] =
    client.secureRequest(ExamEndpoints.getOwnExamsEndpoint)(token)(())

  def openExam(examId: ExamId, token: String): IO[Either[AuthenticationError | OpenExamError, Unit]] =
    client.secureRequest(ExamEndpoints.openExamEndpoint)(token)(examId)

  def closeExam(examId: ExamId, token: String): IO[Either[AuthenticationError | CloseExamError, Unit]] =
    client.secureRequest(ExamEndpoints.closeExamEndpoint)(token)(examId)

  def getQuestions(examId: ExamId, token: String): IO[Either[AuthenticationError | ExamError, List[Question]]] =
    client.secureRequest(QuestionEndpoints.getExamQuestionsEndpoint)(token)(examId)

  def createQuestion(examId: ExamId, form: QuestionForm, token: String)
    : IO[Either[AuthenticationError | AddQuestionError, Question]] =
    client.secureRequest(QuestionEndpoints.addQuestionEndpoint)(token)(examId, form)

  def deleteQuestion(examId: ExamId, questionId: QuestionId, token: String)
    : IO[Either[AuthenticationError | DeleteQuestionError, Unit]] =
    client.secureRequest(QuestionEndpoints.deleteQuestionEndpoint)(token)(examId, questionId)
