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

class ExamService(examRepository: ExamRepository):
  def createExam(examForm: ExamForm, teacherId: TeacherId): IO[Either[ExamCreationError, Exam]] =
    ExamForm
      .validate(examForm)
      .fold(errors => IO.pure(ExamFormValidationError(errors).asLeft), form => createNewExam(form, teacherId))

  def getExamsBy(teacherId: TeacherId): IO[List[Exam]] = examRepository.getExamsBy(teacherId)

  def findById(examId: ExamId): IO[Either[ExamDoesNotExist, Exam]] =
    examRepository
      .getExamById(examId)
      .map(_.toRight(ExamDoesNotExist(examId)))

  def checkPermissions(exam: Exam, teacherId: TeacherId): EitherT[IO, NotAnOwner, Exam] =
    EitherT.fromEither(
      if exam.teacherId == teacherId
      then exam.asRight
      else NotAnOwner(teacherId, exam.id).asLeft
    )

  def openExam(examId: ExamId, teacherId: TeacherId): IO[Either[ExamError, Unit]] =
    (for
      exam <- EitherT(findById(examId))
      _ <- checkPermissions(exam, teacherId)
      _ <- canBeOpened(exam)
      _ <- EitherT.liftF(examRepository.openExamById(exam.id))
    yield ()).value

  def closeExam(examId: ExamId, teacherId: TeacherId): IO[Either[ExamError, Unit]] =
    (for
      exam <- EitherT(findById(examId))
      _ <- checkPermissions(exam, teacherId)
      _ <- canBeClosed(exam)
      _ <- EitherT.liftF(examRepository.closeExamById(exam.id))
    yield ()).value

  private def createNewExam(form: ExamForm, teacherId: TeacherId) = for
    id <- IO.pure(UUID.randomUUID())
    createdExam <- examRepository.createExam(
      NewExam(ExamId(id), form.name, form.description, form.timeLimitMinutes, teacherId)
    )
  yield createdExam

  private def canBeOpened(exam: Exam): EitherT[IO, ExamError, Exam] =
    EitherT.fromEither(
      if exam.status == ExamStatus.DRAFT
      then exam.asRight
      else ExamCannotBeOpened(exam.id, exam.status).asLeft
    )

  private def canBeClosed(exam: Exam): EitherT[IO, ExamError, Exam] =
    EitherT.fromEither(
      if exam.status == ExamStatus.OPEN
      then exam.asRight
      else ExamCannotBeClosed(exam.id, exam.status).asLeft
    )

sealed trait ExamError derives Codec, Schema

case class ExamDoesNotExist(examId: ExamId) extends ExamError derives Codec.AsObject, Schema

sealed trait ExamCreationError extends ExamError derives Codec, Schema
case class ExamFormValidationError(errors: NonEmptyChain[ExamFormError]) extends ExamCreationError

sealed trait ExamStatusError extends ExamError derives Codec, Schema
case class ExamCannotBeOpened(examId: ExamId, examStatus: ExamStatus) extends ExamStatusError
case class ExamCannotBeClosed(examId: ExamId, examStatus: ExamStatus) extends ExamStatusError
case class ExamNotDraft(examId: ExamId, currentStatus: ExamStatus) extends ExamError derives Codec.AsObject, Schema

sealed trait ExamPermissionError extends ExamError derives Codec, Schema
case class NotAnOwner(teacherId: TeacherId, examId: ExamId) extends ExamPermissionError derives Codec.AsObject, Schema
