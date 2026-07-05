package grading

import answers.{Answer, AnswerRepository, MultipleChoice, ShortAnswer, TrueFalse, matchType}
import cats.effect.IO
import cats.syntax.all.*
import exam.{ExamId, ExamRepository}
import question.{
  MultipleChoiceData,
  Question,
  QuestionId,
  QuestionRepository,
  QuestionType,
  ShortAnswerData,
  TrueFalseData
}
import submission.{Submission, SubmissionId, SubmissionRepository, SubmissionStatus}
import answers.GradeAnswerForm
import exam.ExamDoesNotExist
import user.TeacherId
import exam.NotAnOwner
import exam.ExamStatusMismatch
import exam.ExamStatus
import cats.data.EitherT
import question.QuestionNotFound
import answers.AnswerDoesNotExist
import answers.InvalidPointsAwarded
import answers.AnswerType

class GradingService(
  examRepository: ExamRepository,
  questionRepository: QuestionRepository,
  answerRepository: AnswerRepository,
  submissionRepository: SubmissionRepository
):
  def gradeExamAutomatically(examId: ExamId): IO[Unit] =
    for
      _ <- submissionRepository.finishAllInProgressForExam(examId)
      submissions <- submissionRepository.getSubmissionsForExam(examId)
      questions <- questionRepository.getQuestionsByExamOrderedByPosition(examId)
      _ <- submissions.filter(_.status == SubmissionStatus.Finished).traverse(gradeSubmission(_, questions)).void
      _ <- maybeGradeExam(examId)
    yield ()

  def gradeShortAnswer(teacherId: TeacherId, examId: ExamId, submissionId: SubmissionId, questionId: QuestionId, gradeAnswerForm: GradeAnswerForm):
    IO[Either[ExamDoesNotExist | NotAnOwner | ExamStatusMismatch | QuestionNotFound | AnswerDoesNotExist | InvalidPointsAwarded, Answer]] =
      val result: EitherT[IO, ExamDoesNotExist | NotAnOwner | ExamStatusMismatch | QuestionNotFound | AnswerDoesNotExist | InvalidPointsAwarded, Answer] =
        for
          exam <-
            EitherT(examRepository.getExamById(examId).map(_.toRight(ExamDoesNotExist(examId))))
              .ensure(NotAnOwner(teacherId, examId))(_.teacherId == teacherId)
              .ensure(ExamStatusMismatch(examId, "Exam is not closed"))(_.status == ExamStatus.CLOSED)
          question <-
            EitherT(questionRepository.getQuestionById(questionId, examId).map(_.toRight(QuestionNotFound(questionId))))
              .ensure(QuestionNotFound(questionId))(q => q.points >= gradeAnswerForm.points && q.points >= 0)
              .ensure(InvalidPointsAwarded(""))(_.points >= gradeAnswerForm.points)
          answer <-
            EitherT(answerRepository.getAnswer(questionId, submissionId).map(_.toRight(AnswerDoesNotExist(questionId, submissionId))))

          gradedAnswer = answer.copy(pointsAwarded = Some(gradeAnswerForm.points))

          _ <- EitherT.liftF(answerRepository.setPointsAwarded(questionId, submissionId, gradeAnswerForm.points))

          // TODO: grade submission too

          _ <- EitherT.liftF(maybeGradeExam(examId))

        yield gradedAnswer

      result.value

  private def gradeSubmission(submission: Submission, questions: List[Question]): IO[Unit] =
    val questionsById = questions.map(q => q.id -> q).toMap
    for
      answers <- answerRepository.getAllForSubmissionOrderedByQuestionPosition(submission.id)
      gradedPoints <- answers.traverse(gradeAnswerIfAutomatic(submission, questionsById, _))
      _ <- if gradedPoints.forall(_.isDefined) then sealSubmission(submission, gradedPoints.flatten.sum) else IO.unit
    yield ()

  private def gradeAnswerIfAutomatic(
    submission: Submission,
    questionsById: Map[QuestionId, Question],
    answer: Answer
  ): IO[Option[BigDecimal]] =
    questionsById.get(answer.questionId) match
      case Some(question) if question.questionType != QuestionType.ShortAnswer =>
        val points = if isAnsweredCorrectly(question, answer).contains(true) then question.points else BigDecimal(0)
        answerRepository.setPointsAwarded(answer.questionId, submission.id, points).as(Some(points))
      case _ =>
        IO.pure(answer.pointsAwarded)

  private def sealSubmission(submission: Submission, score: BigDecimal): IO[Unit] =
    submissionRepository.gradeSubmission(submission.id, score)

  def maybeGradeExam(examId: ExamId): IO[Unit] =
    for
      submissions <- submissionRepository.getSubmissionsForExam(examId)
      _ <-
        if submissions.forall(_.status == SubmissionStatus.Graded) then examRepository.gradeExamById(examId)
        else IO.unit
    yield ()

  private def isAnsweredCorrectly(question: Question, answer: Answer): Option[Boolean] =
    if !answer.matchType(question) then Some(false)
    else
      (question.data, answer.data) match
        case (TrueFalseData(correctAnswer), TrueFalse(ans)) =>
          Some(ans.contains(correctAnswer))
        case (MultipleChoiceData(_, correctIndex), MultipleChoice(answerIndex)) =>
          Some(correctIndex == answerIndex)
        case (ShortAnswerData(_), ShortAnswer(_)) =>
          None
        case _ =>
          Some(false)
