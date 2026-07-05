package exam

import cats.effect.IO
import cats.syntax.all.*
import cats.data.NonEmptyChain

import java.util.UUID
import io.circe.Codec
import sttp.tapir.Schema
import sttp.tapir.integ.cats.codec.schemaForNec
import cats.data.EitherT
import utils.DerivationConfiguration.given

import user.{TeacherId, StudentId}

class ExamService(examRepository: ExamRepository):
  def createExam(examForm: ExamForm, teacherId: TeacherId): IO[Either[ExamFormValidationError, Exam]] =
    ExamForm
      .validate(examForm)
      .fold(
        errors => IO.pure(ExamFormValidationError(errors).asLeft),
        form => createNewExam(form, teacherId).map(_.asRight)
      )

  def getExamsBy(teacherId: TeacherId): IO[List[Exam]] = examRepository.getExamsBy(teacherId)

  def getOpenExams: IO[List[Exam]] =
    examRepository.getOpenExams

  def findById(examId: ExamId): IO[Either[ExamDoesNotExist, Exam]] =
    examRepository
      .getExamById(examId)
      .map(_.toRight(ExamDoesNotExist(examId)))

  def checkPermissions[E >: NotAnOwner](exam: Exam, teacherId: TeacherId): EitherT[IO, E, Exam] =
    EitherT.fromEither(
      if exam.teacherId == teacherId
      then exam.asRight
      else NotAnOwner(teacherId, exam.id).asLeft
    )

  def openExam(examId: ExamId, teacherId: TeacherId): IO[Either[OpenExamError, Unit]] =
    val result: EitherT[IO, OpenExamError, Unit] = for
      exam <- fetchAndValidateExamByStatus(examId, ExamStatus.DRAFT, "Exam must be in draft status to be opened")
      _ <- checkPermissions(exam, teacherId)
      _ <- EitherT.liftF(examRepository.openExamById(exam.id))
    yield ()
    result.value

  def closeExam(examId: ExamId, teacherId: TeacherId): IO[Either[CloseExamError, Unit]] =
    val result: EitherT[IO, CloseExamError, Unit] = for
      exam <- fetchAndValidateExamByStatus(examId, ExamStatus.OPEN, "Exam must be in open status to be closed")
      _ <- checkPermissions(exam, teacherId)
      _ <- EitherT.liftF(examRepository.closeExamById(exam.id))
    yield ()
    result.value

  def fetchAndValidateExamByStatus(examId: ExamId, expectedStatus: ExamStatus, message: String)
    : EitherT[IO, ExamDoesNotExist | ExamStatusMismatch, Exam] =
    EitherT(findById(examId))
      .ensureOr(exam => ExamStatusMismatch(exam.id, message))(_.status == expectedStatus)

  def fetchAndValidateExamByStatuses(examId: ExamId, expectedStatuses: List[ExamStatus], message: String)
    : EitherT[IO, ExamDoesNotExist | ExamStatusMismatch, Exam] =
    EitherT(findById(examId))
      .ensureOr(exam => ExamStatusMismatch(exam.id, message))(e => expectedStatuses.exists(_ == e.status))

  private def createNewExam(form: ExamForm, teacherId: TeacherId) = for
    id <- IO.pure(UUID.randomUUID())
    createdExam <- examRepository.createExam(
      NewExam(ExamId(id), form.name, form.description, form.timeLimitMinutes, teacherId)
    )
  yield createdExam

sealed trait ExamError derives Codec, Schema

case class ExamDoesNotExist(examId: ExamId) extends ExamError derives Codec.AsObject, Schema

case class ExamFormValidationError(errors: NonEmptyChain[ExamFormError]) extends ExamError
    derives Codec.AsObject,
      Schema

sealed trait ExamStatusError extends ExamError derives Codec, Schema
case class ExamStatusMismatch(examId: ExamId, message: String) extends ExamStatusError derives Codec.AsObject, Schema
case class ExamNotDraft(examId: ExamId, currentStatus: ExamStatus) extends ExamError derives Codec.AsObject, Schema

sealed trait ExamPermissionError extends ExamError derives Codec, Schema
case class NotAnOwner(teacherId: TeacherId, examId: ExamId) extends ExamPermissionError derives Codec.AsObject, Schema
case class NotAStudent(studentId: StudentId) extends ExamPermissionError derives Codec.AsObject, Schema

type OpenExamError = ExamDoesNotExist | NotAnOwner | ExamStatusMismatch
type CloseExamError = ExamDoesNotExist | NotAnOwner | ExamStatusMismatch
