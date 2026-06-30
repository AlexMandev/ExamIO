package question

import cats.data.{EitherT, NonEmptyChain}
import cats.effect.IO
import cats.effect.implicits.{genSpawnOps, genTemporalOps}
import cats.effect.kernel.implicits.monadCancelOps
import cats.syntax.all.*
import io.circe.Codec
import sttp.tapir.Schema
import sttp.tapir.integ.cats.codec.schemaForNec

import java.util.UUID
import exam.{Exam, ExamDoesNotExist, ExamError, ExamId, ExamNotDraft, ExamService, ExamStatus, NotAnOwner, TeacherId}
import utils.DerivationConfiguration.given

sealed trait QuestionError derives Codec, Schema

case class QuestionFormValidationError(errors: NonEmptyChain[QuestionFormError]) extends QuestionError
    derives Codec.AsObject,
      Schema

case class QuestionNotFound(questionId: QuestionId) extends QuestionError derives Codec.AsObject, Schema

type AddQuestionError = ExamDoesNotExist | NotAnOwner | ExamNotDraft | QuestionFormValidationError
type DeleteQuestionError = ExamDoesNotExist | NotAnOwner | ExamNotDraft | QuestionNotFound

class QuestionService(questionRepository: QuestionRepository, examService: ExamService):
  def getQuestionsForExam(userId: UUID, examId: ExamId): IO[Either[ExamError, List[Question]]] =
    val result: EitherT[IO, ExamError, List[Question]] = for
      exam <- EitherT(examService.findById(examId))
      _ <- examService.checkPermissions(exam, TeacherId(userId))
      questions <- EitherT.liftF(questionRepository.getForExam(examId))
    yield questions

    result.value

  def addQuestion(
    form: QuestionForm,
    examId: ExamId,
    teacherId: TeacherId
  ): IO[Either[AddQuestionError, Question]] =
    val result: EitherT[IO, AddQuestionError, Question] = for
      exam <- EitherT(examService.findById(examId))
      _ <- examService.checkPermissions(exam, teacherId)
      _ <- checkDraft(exam)
      validated <- validateNewQuestionForm(form)
      position <- EitherT.liftF(questionRepository.nextPosition(examId))
      id <- EitherT.liftF(IO(UUID.randomUUID()))
      q <- EitherT.liftF(questionRepository.addQuestion(buildQuestion(validated, examId, position, id)))
    yield q
    result.value

  def deleteQuestion(
    questionId: QuestionId,
    examId: ExamId,
    teacherId: TeacherId
  ): IO[Either[DeleteQuestionError, Unit]] =
    val result: EitherT[IO, DeleteQuestionError, Unit] = for
      exam <- EitherT(examService.findById(examId))
      _ <- examService.checkPermissions(exam, teacherId)
      _ <- checkDraft(exam)
      found <- EitherT.liftF(questionRepository.deleteQuestion(questionId, examId))
      _ <- EitherT.fromEither(if found then Right(()) else Left(QuestionNotFound(questionId)))
    yield ()
    result.value

  private def checkDraft[E >: ExamNotDraft](exam: Exam): EitherT[IO, E, Unit] =
    EitherT.fromEither(
      if exam.status == ExamStatus.DRAFT then Right(())
      else Left(ExamNotDraft(exam.id, exam.status))
    )

  private def validateNewQuestionForm(form: QuestionForm): EitherT[IO, AddQuestionError, QuestionForm] =
    EitherT.fromEither(
      QuestionForm.validate(form).toEither.left.map(QuestionFormValidationError(_))
    )

  private def buildQuestion(form: QuestionForm, examId: ExamId, position: Int, id: UUID): Question =
    val questionType = form.data match
      case _: MultipleChoiceData => QuestionType.MultipleChoice
      case _: TrueFalseData => QuestionType.TrueFalse
      case _: ShortAnswerData => QuestionType.ShortAnswer
    Question(QuestionId(id), examId, form.questionText, questionType, form.points, position, form.data)
