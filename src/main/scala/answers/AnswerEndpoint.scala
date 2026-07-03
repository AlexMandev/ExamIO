package answer

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.integ.cats.codec.*
import sttp.model.StatusCode
import sttp.model.StatusCode.{Created, NotFound}

import infrastructure.ExamIOEndpoints.{secure, apiBaseEndpoint}

import utils.jsonBodyTypedError
import user.UserRole
import question.QuestionId
import exam.ExamId
import submission.SubmissionId

object AnswerEndpoint:
  private val answerBaseEndpoint = apiBaseEndpoint
    .in("exams" / path[ExamId]("examId"))
    .in("submissions" / path[SubmissionId]("submissionId"))
    .in("answers" / path[QuestionId]("questionId")).tag("Answers")

  def getAnswerEndpoint = answerBaseEndpoint
    // TODO: this probably won't allow for the teacher to access answers
    .secure(UserRole.STUDENT,
      oneOf[AnswerError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[AnswerDoesNotExist]))
      )
    )
    .out(jsonBody[Answer])
    .get

  def addAnswerEndpoint = answerBaseEndpoint
    .secure(UserRole.STUDENT)
    .in(jsonBody[AnswerForm])
    .out(statusCode(Created).and(jsonBody[Answer]))
    .post

  def clearAnswerEndpoint = answerBaseEndpoint
    .secure(UserRole.STUDENT,
      oneOf[AnswerError](
        oneOfVariant(statusCode(NotFound).and(jsonBodyTypedError[AnswerDoesNotExist]))
      )
    )
    .delete
