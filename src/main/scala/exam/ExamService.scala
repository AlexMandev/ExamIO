package exam

import cats.effect.IO
import cats.syntax.all.*
import cats.data.NonEmptyChain
import java.util.UUID
import io.circe.Codec
import sttp.tapir.Schema
import sttp.tapir.integ.cats.codec.schemaForNec

class ExamService(examRepository: ExamRepository):
  def createExam(examForm: ExamForm, teacherId: UUID): IO[Either[ExamCreationError, Exam]] =
    ExamForm
      .validate(examForm)
      .fold(errors => IO.pure(ExamFormValidationError(errors).asLeft), form => createNewExam(form, teacherId))

  private def createNewExam(form: ExamForm, teacherId: UUID) = for
    id <- IO.pure(UUID.randomUUID())
    createdExam <- examRepository.createExam(
      NewExam(id, form.name, form.description, form.timeLimitMinutes, teacherId)
    )
  yield createdExam

sealed trait ExamCreationError derives Codec, Schema
case class ExamFormValidationError(errors: NonEmptyChain[ExamFormError]) extends ExamCreationError
