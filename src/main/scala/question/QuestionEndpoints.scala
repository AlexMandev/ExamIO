package question

import exam.{ExamError, ExamId, ExamDoesNotExist, NotAnOwner}
import sttp.tapir.*
import sttp.tapir.json.circe.jsonBody
import sttp.model.StatusCode.{Created, NotFound}
import infrastructure.ExamIOEndpoints.*
import user.UserRole.TEACHER

type SomeType = String

object QuestionEndpoints:
  private val questionsBaseEndpoint = apiBaseEndpoint
    .in("exams" / path[ExamId] / "questions")
    .tag("Questions")

  private val securedBaseForTeachers =
    questionsBaseEndpoint
      .secure(TEACHER)
      .errorOutVariant(oneOfVariant(statusCode(NotFound).and(jsonBody[ExamDoesNotExist])))
      .errorOutVariant(oneOfVariant(statusCode(Forbidden).and(jsonBody[NotAnOwner])))

  val getExamQuestionsEndpoint = questionsBaseEndpoint.out(jsonBody[List[Question]]).get

  val addQuestionEndpoint =
    securedBaseForTeachers
      .in(jsonBody[QuestionForm])
      .post

  val deleteQuestionEndpoint = securedBaseForTeachers
    .errorOutVariant(oneOfVariant(statusCode(NotFound).and(jsonBody[QuestionNotFound])))
    .errorOutVariant(oneOfVariant(statusCode(Conflict).and(jsonBody[ExamNotDraft])))
    .in(path[QuestionId]("questionId"))
    .delete
