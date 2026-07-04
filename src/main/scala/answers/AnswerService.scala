package answer

import cats.effect.IO
import cats.implicits.*
import cats.data.{EitherT, NonEmptyChain}

import io.circe.Codec
import sttp.tapir.Schema
import sttp.tapir.integ.cats.codec.schemaForNec

import java.util.UUID

import question.{QuestionId, QuestionService, QuestionError, QuestionType}
import submission.{SubmissionId, SubmissionService, SubmissionError, AlreadySubmitted, SubmissionStatus}
import exam.{ExamId, ExamError, ExamService, ExamStatus, ExamCannotBeOpened}
import user.StudentId

type AnswerServiceError = AnswerError | QuestionError | SubmissionError | ExamError

class AnswerService(
  answerRepository: AnswerRepository,
  questionService: QuestionService,
  submissionService: SubmissionService,
  examService: ExamService
):

  def getAnswer(userId: UUID, questionId: QuestionId, submissionId: SubmissionId, examId: ExamId): IO[Either[AnswerServiceError, Answer]] =
    val result: EitherT[IO, AnswerServiceError, Answer] =
      for
        _ <- EitherT(questionService.getQuestionById(questionId, examId))
        _ <- EitherT(submissionService.getSubmissionById(submissionId, examId, StudentId(userId)))

        answer <- EitherT(answerRepository.getAnswer(questionId, submissionId)
                    .map(_.toRight(AnswerDoesNotExist(questionId, submissionId))))

      yield answer

    result.value

  def addAnswer(studentId: StudentId, answerForm: AnswerForm, questionId: QuestionId, submissionId: SubmissionId, examId: ExamId): IO[Either[AnswerServiceError, Answer]] =
    addAnswerT(studentId, answerForm, questionId, submissionId, examId).value


  def clearAnswer(studentId: StudentId, questionId: QuestionId, submissionId: SubmissionId, examId: ExamId): IO[Either[AnswerServiceError, Unit]] =
    (
      for
        _ <- EitherT(getAnswer(studentId.value, questionId, submissionId, examId))
        _ <- EitherT.liftF(
          answerRepository.clearAnswer(questionId, submissionId)
        )
      yield ()
    ).value


  private def addAnswerT(studentId: StudentId, answerForm: AnswerForm, questionId: QuestionId, submissionId: SubmissionId, examId: ExamId): EitherT[IO, AnswerServiceError, Answer] =
    for
      _ <- EitherT(examService.findById(examId))
            .ensureOr(exam => ExamCannotBeOpened(exam.id, exam.status))(_.status == ExamStatus.OPEN)

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
        answerType = validatedForm.answerData.toType,
        data = validatedForm.answerData
      )

      _ <- EitherT.pure(answer)
            .ensure(AnswerTypeMismatch(answer.answerType, question.questionType))
                   (_.matchType(question))

      savedAnswer <-
        EitherT.liftF(answerRepository.addAnswer(answer))

    yield savedAnswer

sealed trait AnswerError derives Codec, Schema

case class AnswerDoesNotExist(questionId: QuestionId, submissionId: SubmissionId)
  extends AnswerError derives Codec.AsObject, Schema

case class AnswerTypeMismatch(answerType: AnswerType, questionType: QuestionType)
  extends AnswerError derives Codec.AsObject, Schema

case class AnswerFormValidationError(errors: NonEmptyChain[AnswerFormError]) extends AnswerError
    derives Codec.AsObject,
      Schema
