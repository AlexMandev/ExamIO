package question

import cats.data.{EitherT, NonEmptyChain}
import cats.effect.IO
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

class QuestionService(questionRepository: QuestionRepository, examService: ExamService):

  def addQuestion(
    form: QuestionForm,
    examId: ExamId,
    teacherId: TeacherId
  ): IO[Either[ExamError | QuestionError, Question]] =
    val result: EitherT[IO, ExamError | QuestionError, Question] = for
      exam <- EitherT(examService.findById(examId))
      _ <- examService.checkPermissions(exam, teacherId)
      _ <- checkDraft(exam)
      validated <- validateNewQuestionForm(form)
      position <- EitherT.liftF(questionRepository.nextPosition(examId))
      id <- EitherT.liftF(IO(UUID.randomUUID()))
      q <- EitherT.liftF(questionRepository.addQuestion(buildQuestion(validated, examId, position, id)))
    yield q
    result.value

  private def checkDraft(exam: Exam): EitherT[IO, ExamError | QuestionError, Unit] =
    EitherT.fromEither(
      if exam.status == ExamStatus.DRAFT then Right(())
      else Left(ExamNotDraft(exam.id, exam.status))
    )

  private def validateNewQuestionForm(form: QuestionForm): EitherT[IO, ExamError | QuestionError, QuestionForm] =
    EitherT.fromEither(
      QuestionForm.validate(form).toEither.left.map(QuestionFormValidationError(_))
    )

  private def buildQuestion(form: QuestionForm, examId: ExamId, position: Int, id: UUID): Question =
    val questionType = form.data match
      case _: MultipleChoiceData => QuestionType.MultipleChoice
      case _: TrueFalseData => QuestionType.TrueFalse
      case _: ShortAnswerData => QuestionType.ShortAnswer
    Question(QuestionId(id), examId, form.questionText, questionType, form.points, position, form.data)
