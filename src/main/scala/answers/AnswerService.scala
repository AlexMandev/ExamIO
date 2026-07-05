package answers

import cats.effect.IO
import cats.implicits.*
import cats.data.{EitherT, NonEmptyChain}

import io.circe.Codec
import sttp.tapir.Schema
import sttp.tapir.integ.cats.codec.schemaForNec

import question.{QuestionId, QuestionService, QuestionNotFound, QuestionType}
import submission.{
  SubmissionId,
  SubmissionService,
  SubmissionDoesNotExist,
  NotSubmissionOwner,
  AlreadySubmitted,
  SubmissionStatus,
  SubmissionNotInExam
}
import exam.{ExamId, ExamDoesNotExist, ExamStatusMismatch, ExamService, ExamStatus, NotAnOwner}
import user.{StudentId, TeacherId, UserRole}
import infrastructure.auth.AuthenticatedUser

type AnswerServiceError =
  AnswerDoesNotExist | AnswerTypeMismatch | AnswerFormValidationError | QuestionNotFound | SubmissionDoesNotExist |
    SubmissionNotInExam | NotSubmissionOwner | AlreadySubmitted | ExamDoesNotExist | ExamStatusMismatch | NotAnOwner

class AnswerService(
  answerRepository: AnswerRepository,
  questionService: QuestionService,
  submissionService: SubmissionService,
  examService: ExamService
):

  def getAnswer(user: AuthenticatedUser, questionId: QuestionId, submissionId: SubmissionId, examId: ExamId)
    : IO[Either[AnswerServiceError, Answer]] =
    val result: EitherT[IO, AnswerServiceError, Answer] =
      for
        _ <- EitherT(questionService.getQuestionById(questionId, examId))
        _ <- checkSubmissionAccess(user, submissionId, examId)

        answer <- EitherT(
          answerRepository
            .getAnswer(questionId, submissionId)
            .map(_.toRight(AnswerDoesNotExist(questionId, submissionId)))
        )
      yield answer

    result.value

  private def checkSubmissionAccess(user: AuthenticatedUser, submissionId: SubmissionId, examId: ExamId)
    : EitherT[IO, AnswerServiceError, Unit] =
    user.role match
      case UserRole.STUDENT =>
        EitherT(submissionService.getSubmissionById(submissionId, examId, StudentId(user.id))).void
      case UserRole.TEACHER =>
        for
          exam <- EitherT(examService.findById(examId))
          _ <- examService.checkPermissions(exam, TeacherId(user.id))
        yield ()

  def addAnswer(
    studentId: StudentId,
    answerForm: AnswerForm,
    questionId: QuestionId,
    submissionId: SubmissionId,
    examId: ExamId
  ): IO[Either[AnswerServiceError, Answer]] =
    addAnswerT(studentId, answerForm, questionId, submissionId, examId).value

  def clearAnswer(studentId: StudentId, questionId: QuestionId, submissionId: SubmissionId, examId: ExamId)
    : IO[Either[AnswerServiceError, Unit]] =
    (
      for
        _ <- EitherT(getAnswer(AuthenticatedUser(studentId.value, UserRole.STUDENT), questionId, submissionId, examId))
        _ <- EitherT.liftF(
          answerRepository.clearAnswer(questionId, submissionId)
        )
      yield ()
    ).value

  private def addAnswerT(
    studentId: StudentId,
    answerForm: AnswerForm,
    questionId: QuestionId,
    submissionId: SubmissionId,
    examId: ExamId
  ): EitherT[IO, AnswerServiceError, Answer] =
    for
      _ <- examService.fetchAndValidateExamByStatus(examId, ExamStatus.OPEN, "Exam must be open to submit answers")

      question <- EitherT(questionService.getQuestionById(questionId, examId))

      _ <- EitherT(submissionService.getSubmissionById(submissionId, examId, studentId))
        .ensureOr(sub => AlreadySubmitted(studentId, examId, sub.id))(_.status == SubmissionStatus.InProgress)

      validatedForm <-
        EitherT.fromEither[IO](
          AnswerForm
            .validate(answerForm, AnswerValidationContext.fromQuestion(question))
            .toEither
            .leftMap(errors => AnswerFormValidationError(errors))
        )

      answer = Answer(
        questionId = questionId,
        submissionId = submissionId,
        data = validatedForm.answerData
      )

      _ <- EitherT
        .pure(answer)
        .ensure(AnswerTypeMismatch(answer.data.toType, question.questionType))(_.matchType(question))

      savedAnswer <-
        EitherT.liftF(answerRepository.addAnswer(answer))
    yield savedAnswer

sealed trait AnswerError derives Codec, Schema

case class AnswerDoesNotExist(questionId: QuestionId, submissionId: SubmissionId) extends AnswerError
    derives Codec.AsObject,
      Schema

case class AnswerTypeMismatch(answerType: AnswerType, questionType: QuestionType) extends AnswerError
    derives Codec.AsObject,
      Schema

case class AnswerFormValidationError(errors: NonEmptyChain[AnswerFormError]) extends AnswerError
    derives Codec.AsObject,
      Schema
