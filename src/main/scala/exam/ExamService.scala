package exam

import cats.effect.IO
import cats.syntax.all.*
import cats.data.NonEmptyChain
import java.util.UUID
import io.circe.Codec
import sttp.tapir.Schema
import sttp.tapir.integ.cats.codec.schemaForNec
import cats.data.EitherT
import infrastructure.config.AppConfig.loadConfig

class ExamService(examRepository: ExamRepository):
  def createExam(examForm: ExamForm, teacherId: UUID): IO[Either[ExamCreationError, Exam]] =
    ExamForm
      .validate(examForm)
      .fold(errors => IO.pure(ExamFormValidationError(errors).asLeft), form => createNewExam(form, teacherId))

  def getExamsBy(teacherId: UUID): IO[List[Exam]] = examRepository.getExamsBy(teacherId)

  def openExam(examId: UUID, teacherId: UUID): IO[Either[ExamError, Unit]] =
    (for
      exam <- EitherT(loadExam(examId))
      _ <- checkPermissions(exam, teacherId)
      _ <- canBeOpened(exam)
      _ <- EitherT(examRepository.openExamById(exam.id).map(_.asRight))
    yield ()).value

  def closeExam(examId: UUID, teacherId: UUID): IO[Either[ExamError, Unit]] =
    (for
      exam <- EitherT(loadExam(examId))
      _ <- checkPermissions(exam, teacherId)
      _ <- canBeClosed(exam)
      _ <- EitherT(examRepository.closeExamById(exam.id).map(_.asRight))
    yield ()).value

  private def createNewExam(form: ExamForm, teacherId: UUID) = for
    id <- IO.pure(UUID.randomUUID())
    createdExam <- examRepository.createExam(
      NewExam(id, form.name, form.description, form.timeLimitMinutes, teacherId)
    )
  yield createdExam

  private def loadExam(examId: UUID): IO[Either[ExamError, Exam]] =
    examRepository
      .getExamById(examId)
      .map(maybeExam =>
        maybeExam match
          case None => ExamDoesNotExist(examId).asLeft
          case Some(exam) => exam.asRight
      )

  private def checkPermissions(exam: Exam, teacherId: UUID): EitherT[IO, ExamError, Exam] =
    EitherT.fromEither(
      if exam.teacherId == teacherId
      then exam.asRight
      else NotAnOwner(teacherId, exam.id).asLeft
    )

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

case class ExamDoesNotExist(examId: UUID) extends ExamError

sealed trait ExamCreationError extends ExamError derives Codec, Schema
case class ExamFormValidationError(errors: NonEmptyChain[ExamFormError]) extends ExamCreationError

sealed trait ExamStatusError extends ExamError derives Codec, Schema
case class ExamCannotBeOpened(examId: UUID, examStatus: ExamStatus) extends ExamStatusError
case class ExamCannotBeClosed(examId: UUID, examStatus: ExamStatus) extends ExamStatusError

sealed trait ExamPermissionError extends ExamError derives Codec, Schema
case class NotAnOwner(teacherId: UUID, examId: UUID) extends ExamPermissionError
