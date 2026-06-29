package question

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.integ.cats.codec.*
import sttp.model.StatusCode.{BadRequest, Conflict, Created, Forbidden, NotFound}
import infrastructure.ExamIOEndpoints.*
import user.UserRole.TEACHER
import exam.{ExamId, NotAnOwner}

object QuestionEndpoints:
  private val questionsBaseEndpoint = apiBaseEndpoint
    .in("exams" / path[ExamId]("examId") / "questions")
    .tag("Questions")

  private val teachersBaseSecureEndpoint = questionsBaseEndpoint.secure(TEACHER)

  val getExamQuestionsEndpoint = questionsBaseEndpoint.out(jsonBody[List[Question]]).get

  val addQuestionEndpoint =
    questionsBaseEndpoint
      .secure(TEACHER)
      .in(jsonBody[QuestionForm])
      .errorOutVariant(oneOfVariant(statusCode(NotFound).and(jsonBody[ExamNotFound])))
      .errorOutVariant(oneOfVariant(statusCode(Forbidden).and(jsonBody[NotAnOwner])))
      .errorOutVariant(oneOfVariant(statusCode(BadRequest).and(jsonBody[QuestionFormValidationError])))
      .errorOutVariant(oneOfVariant(statusCode(Conflict).and(jsonBody[ExamNotDraft])))
      .out(statusCode(Created).and(jsonBody[Question]))
      .post
