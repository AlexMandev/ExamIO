package client

import cats.effect.IO
import exam.{
  CloseExamError,
  Exam,
  ExamDoesNotExist,
  ExamEndpoints,
  ExamError,
  ExamForm,
  ExamFormValidationError,
  ExamId,
  ExamStatusMismatch,
  NotAnOwner,
  OpenExamError
}
import infrastructure.auth.AuthenticationError
import question.{AddQuestionError, DeleteQuestionError, PublicQuestion, Question, QuestionEndpoints, QuestionForm, QuestionId, QuestionNotFound}
import submission.{Submission, SubmissionEndpoints, SubmissionError, SubmissionId}
import answers.{Answer, AnswerDoesNotExist, AnswerEndpoints, AnswerForm, AnswerServiceError, GradeAnswerForm, InvalidPointsAwarded}
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

  def listOpenExams(token: String): IO[Either[AuthenticationError, List[Exam]]] =
    client.secureRequest(ExamEndpoints.getOpenExamsEndpoint)(token)(())

  def getMySubmissions(token: String): IO[Either[AuthenticationError, List[Submission]]] =
    client.secureRequest(SubmissionEndpoints.getMySubmissionsEndpoint)(token)(())

  def createSubmission(examId: ExamId, token: String)
    : IO[Either[AuthenticationError | ExamError | SubmissionError, Submission]] =
    client.secureRequest(SubmissionEndpoints.createSubmissionEndpoint)(token)(examId)

  def finishSubmission(examId: ExamId, submissionId: SubmissionId, token: String)
    : IO[Either[AuthenticationError | ExamError | SubmissionError, Submission]] =
    client.secureRequest(SubmissionEndpoints.finishSubmissionEndpoint)(token)(examId, submissionId)

  def getResult(examId: ExamId, submissionId: SubmissionId, token: String)
    : IO[Either[AuthenticationError | ExamError | SubmissionError, Submission]] =
    client.secureRequest(SubmissionEndpoints.getResultEndpoint)(token)(examId, submissionId)

  def getStudentQuestions(examId: ExamId, submissionId: SubmissionId, token: String)
    : IO[Either[AuthenticationError | ExamError | SubmissionError, List[PublicQuestion]]] =
    client.secureRequest(QuestionEndpoints.getStudentQuestionsEndpoint)(token)(examId, submissionId)

  def getAnswer(examId: ExamId, submissionId: SubmissionId, questionId: QuestionId, token: String)
    : IO[Either[AuthenticationError | AnswerServiceError, Answer]] =
    client.secureRequest(AnswerEndpoints.getAnswerEndpoint)(token)(examId, submissionId, questionId)

  def addAnswer(examId: ExamId, submissionId: SubmissionId, questionId: QuestionId, form: AnswerForm, token: String)
    : IO[Either[AuthenticationError | AnswerServiceError, Answer]] =
    client.secureRequest(AnswerEndpoints.addAnswerEndpoint)(token)(examId, submissionId, questionId, form)

  def clearAnswer(examId: ExamId, submissionId: SubmissionId, questionId: QuestionId, token: String)
    : IO[Either[AuthenticationError | AnswerServiceError, Unit]] =
    client.secureRequest(AnswerEndpoints.clearAnswerEndpoint)(token)(examId, submissionId, questionId)

  def getResults(examId: ExamId, token: String): IO[Either[AuthenticationError | ExamError, List[Submission]]] =
    client.secureRequest(SubmissionEndpoints.getResultsEndpoint)(token)(examId)

  def gradeAnswer(examId: ExamId, submissionId: SubmissionId, questionId: QuestionId, form: GradeAnswerForm, token: String): IO[
    Either[
      AuthenticationError | ExamDoesNotExist | NotAnOwner | ExamStatusMismatch | QuestionNotFound | AnswerDoesNotExist |
        InvalidPointsAwarded,
      Answer
    ]
  ] =
    client.secureRequest(AnswerEndpoints.gradeAnswerEndpoint)(token)(examId, submissionId, questionId, form)
