package answer

import cats.effect.IO
import cats.implicits.*
import cats.data.EitherT

import io.circe.Codec
import sttp.tapir.Schema

import question.{QuestionId, QuestionService, QuestionError}
import submission.{SubmissionId, SubmissionService}

class AnswerService(answerRepository: AnswerRepository, questionService: QuestionService, submissionService: SubmissionService):
  // TODO: validation if submission/question exists

  def getAnswer(questionId: QuestionId, submissionId: SubmissionId): IO[Either[AnswerError, Answer]] =
      answerRepository.getAnswer(questionId, submissionId)
        .toRight(AnswerDoesNotExist(questionId, submissionId))

  def addAnswer(answerForm: AnswerForm, questionId: QuestionId): IO[Either[AnswerError | QuestionError, Answer]] =
    val result =
      for
        maybeQuestion <- EitherT(questionService.getQuestionById(questionId))

      yield ???

    result.value


  def clearAnswer(questionId: QuestionId, submissionId: SubmissionId): IO[Either[AnswerError, Unit]] =
    (EitherT(getAnswer(questionId, submissionId)) >>
      EitherT(answerRepository.clearAnswer(questionId, submissionId)))
        .map(_ => ())
        .value

sealed trait AnswerError derives Codec, Schema
case class AnswerDoesNotExist(questionId: QuestionId, submissionId: SubmissionId)
  extends AnswerError derives Codec.AsObject, Schema
