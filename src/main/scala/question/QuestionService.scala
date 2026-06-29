package question

import cats.data.{EitherT, NonEmptyChain}
import cats.effect.IO
import cats.syntax.all.*
import io.circe.Codec
import sttp.tapir.Schema
import sttp.tapir.integ.cats.codec.schemaForNec
import java.util.UUID

import exam.{Exam, ExamId, ExamRepository, ExamStatus, TeacherId}
import utils.DerivationConfiguration.given

sealed trait QuestionError derives Codec, Schema
case class ExamNotFound(examId: ExamId) extends QuestionError
case class NotExamOwner(teacherId: TeacherId, examId: ExamId) extends QuestionError
case class ExamNotDraft(examId: ExamId, currentStatus: ExamStatus) extends QuestionError
case class QuestionFormValidationError(errors: NonEmptyChain[QuestionFormError]) extends QuestionError

class QuestionService(questionRepository: QuestionRepository, examRepository: ExamRepository):
  def addQuestion(form: QuestionForm, examId: ExamId, teacherId: TeacherId): IO[Either[QuestionError, Question]] =
    (for
      exam <- loadExam(examId)
      _ <- checkOwnership(exam, teacherId)
      _ <- checkDraft(exam)
      validated <- validateForm(form)
      position <- EitherT.liftF(questionRepository.nextPosition(examId))
      id <- EitherT.liftF(IO(UUID.randomUUID()))
      result <- EitherT.liftF(questionRepository.addQuestion(buildQuestion(validated, examId, position, id)))
    yield result).value

  private def loadExam(examId: ExamId): EitherT[IO, QuestionError, Exam] =
    EitherT(examRepository.getExamById(examId).map(_.toRight(ExamNotFound(examId))))

  private def checkOwnership(exam: Exam, teacherId: TeacherId): EitherT[IO, QuestionError, Unit] =
    EitherT.fromEither(
      if exam.teacherId == teacherId then Right(())
      else Left(NotExamOwner(teacherId, exam.id))
    )

  private def checkDraft(exam: Exam): EitherT[IO, QuestionError, Unit] =
    EitherT.fromEither(
      if exam.status == ExamStatus.DRAFT then Right(())
      else Left(ExamNotDraft(exam.id, exam.status))
    )

  private def validateForm(form: QuestionForm): EitherT[IO, QuestionError, QuestionForm] =
    EitherT.fromEither(
      QuestionForm.validate(form).toEither.left.map(QuestionFormValidationError(_))
    )

  private def buildQuestion(form: QuestionForm, examId: ExamId, position: Int, id: UUID): Question =
    val questionType = form.data match
      case _: MultipleChoiceData => QuestionType.MultipleChoice
      case _: TrueFalseData      => QuestionType.TrueFalse
      case _: ShortAnswerData    => QuestionType.ShortAnswer
    Question(QuestionId(id), examId, form.questionText, questionType, form.points, position, form.data)
