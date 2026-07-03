package client

import cats.effect.IO
import exam.{Exam, ExamEndpoints, ExamForm, ExamFormValidationError, ExamId}
import infrastructure.auth.AuthenticationError
import question.{AddQuestionError, Question, QuestionEndpoints, QuestionForm}
import user.{LoginResponse, User, UserEndpoints, UserLoginForm, UserRegistrationError, UserRegistrationForm}

class ExamIOApiClient(client: ApiClient):
  def login(form: UserLoginForm): IO[Either[Unit, LoginResponse]] = client.request(UserEndpoints.loginEndpoint)(form)

  def register(form: UserRegistrationForm): IO[Either[UserRegistrationError, User]] =
    client.request(UserEndpoints.registerUserEndpoint)(form)

  def createExam(form: ExamForm, token: String): IO[Either[AuthenticationError | ExamFormValidationError, Exam]] =
    client.secureRequest(ExamEndpoints.createExamEndpoint)(token)(form)

  def createQuestion(examId: ExamId, form: QuestionForm, token: String)
    : IO[Either[AuthenticationError | AddQuestionError, Question]] =
    client.secureRequest(QuestionEndpoints.addQuestionEndpoint)(token)(examId, form)
