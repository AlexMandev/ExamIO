package client

import cats.effect.IO
import cats.syntax.all.*
import exam.{
  Exam,
  ExamDoesNotExist,
  ExamForm,
  ExamFormError,
  ExamFormValidationError,
  ExamId,
  ExamStatusMismatch,
  InvalidDescriptionError,
  InvalidNameError,
  InvalidTimeLimitError,
  NotAnOwner
}
import question.{
  InvalidPoints,
  InvalidQuestionData,
  InvalidQuestionText,
  MultipleChoiceData,
  Question,
  QuestionData,
  QuestionForm,
  QuestionFormError,
  QuestionFormValidationError,
  QuestionType,
  ShortAnswerData,
  TrueFalseData
}
import submission.{Submission, SubmissionId}
import answers.{AnswerData, GradeAnswerForm, MultipleChoice, ShortAnswer, TrueFalse}
import CommonFlow.*
import client.utils.OptionUtils.*

class TeacherFlow(client: ExamIOApiClient, token: String):
  def run(preliminaryMessage: Option[String] = None): IO[Unit] =
    recoverToMenu(
      for
        _ <- clearConsole
        _ <- preliminaryMessage.printLn
        _ <- displayMenu
        command <- promptForString("> ").map(_.trim)
        _ <- command match
          case "1" => createExamFlow.flatMap(msg => run(Some(msg)))
          case "2" => listExamsFlow >> run()
          case "3" => IO.println("Logged out.") >> pressEnterToContinue
          case _ => run()
      yield (),
      retry = run()
    )

  private def displayMenu: IO[Unit] =
    IO.println(
      """|=== Menu ===
         |1. Create exam
         |2. My exams
         |3. Logout
         |""".stripMargin
    )

  private def createExamFlow: IO[String] =
    for
      _ <- clearConsole
      _ <- IO.println("=== Create Exam ===")
      name <- promptForString("Exam name: ")
      description <- promptForString("Description (blank to skip): ").map(s => Option(s.trim).filter(_.nonEmpty))
      timeLimit <- promptForInt("Time limit (minutes): ")
      result <- client.createExam(ExamForm(name, description, timeLimit), token)
      msg = result.fold(
        {
          case ExamFormValidationError(errs) =>
            "Validation errors:\n" + errs.toList.map(e => s"  - ${formatExamFormError(e)}").mkString("\n")
          case other => s"Error: $other"
        },
        exam => s"Exam '${exam.name}' created!"
      )
    yield msg

  private def listExamsFlow: IO[Unit] =
    for
      result <- client.getOwnExams(token)
      _ <- result.fold(
        err => IO.println(s"Error: $err") >> pressEnterToContinue,
        exams =>
          if exams.isEmpty then IO.println("No exams yet.") >> pressEnterToContinue
          else examListMenu(exams)
      )
    yield ()

  private def examListMenu(exams: List[Exam]): IO[Unit] =
    for
      _ <- clearConsole
      _ <- displayExamList(exams)
      input <- promptForString("Select exam (0 to go back): ").map(_.trim)
      _ <- input.toIntOption match
        case Some(0) => ().pure[IO]
        case Some(n) if n >= 1 && n <= exams.length => examMenu(exams(n - 1)) >> listExamsFlow
        case _ => examListMenu(exams)
    yield ()

  private def displayExamList(exams: List[Exam]): IO[Unit] =
    IO.println("=== Your Exams ===") >> IO.println(
      exams.zipWithIndex
        .map { case (exam, i) => s"${i + 1}. ${exam.name} [${exam.status}]" }
        .mkString("\n")
    )

  private def examMenu(exam: Exam, preliminaryMessage: Option[String] = None): IO[Unit] =
    for
      _ <- clearConsole
      _ <- preliminaryMessage.printLn
      _ <- displayExamMenu(exam)
      command <- promptForString("> ").map(_.trim)
      _ <- command match
        case "1" => viewQuestionsFlow(exam.id) >> examMenu(exam)
        case "2" => addQuestionFlow(exam.id).flatMap(msg => examMenu(exam, Some(msg)))
        case "3" => deleteQuestionFlow(exam.id).flatMap(msg => examMenu(exam, Option.when(msg.nonEmpty)(msg)))
        case "4" => openExamFlow(exam.id)
        case "5" => closeExamFlow(exam.id)
        case "6" => viewResultsFlow(exam.id) >> examMenu(exam)
        case "7" => gradeShortAnswersFlow(exam.id) >> examMenu(exam)
        case "8" => ().pure[IO]
        case _ => examMenu(exam)
    yield ()

  private def displayExamMenu(exam: Exam): IO[Unit] =
    IO.println(
      s"""|=== ${exam.name} [${exam.status}] ===
          |1. View questions
          |2. Add question
          |3. Delete question
          |4. Open exam
          |5. Close exam
          |6. View results
          |7. Grade short answers
          |8. Back
          |""".stripMargin
    )

  private def viewQuestionsFlow(examId: ExamId): IO[Unit] =
    for
      _ <- clearConsole
      result <- client.getQuestions(examId, token)
      _ <- result.fold(
        err => IO.println(s"Error: $err"),
        questions =>
          if questions.isEmpty then IO.println("No questions yet.")
          else
            IO.println(
              questions.zipWithIndex
                .map { case (q, i) => s"${i + 1}. [${q.questionType}] ${q.questionText} (${q.points} pts)" }
                .mkString("\n")
            )
      )
      _ <- pressEnterToContinue
    yield ()

  private def addQuestionFlow(examId: ExamId): IO[String] =
    for
      _ <- clearConsole
      _ <- IO.println("=== Add Question ===")
      text <- promptForString("Question text: ")
      points <- promptForDecimal("Points: ")
      data <- collectQuestionData
      result <- client.createQuestion(examId, QuestionForm(text, points, data), token)
      msg = result.fold(
        {
          case QuestionFormValidationError(errs) =>
            "Validation errors:\n" + errs.toList.map(e => s"  - ${formatQuestionFormError(e)}").mkString("\n")
          case other => s"Failed: $other"
        },
        q => s"Question added at position ${q.position}."
      )
    yield msg

  private def collectQuestionData: IO[QuestionData] =
    for
      _ <- IO.println("Type: 1. Multiple Choice  2. True/False  3. Short Answer")
      typeCmd <- promptForString("> ").map(_.trim)
      data <- typeCmd match
        case "1" => collectMultipleChoiceData
        case "2" => collectTrueFalseData
        case "3" => collectShortAnswerData
        case _ => IO.println("Invalid type.") >> collectQuestionData
    yield data

  private def collectMultipleChoiceData: IO[QuestionData] =
    for
      _ <- IO.println("Enter options one by one, empty line when done (min 2):")
      options <- collectOptions(List.empty)
      _ <- IO.println(options.zipWithIndex.map { case (opt, i) => s"  ${i + 1}. $opt" }.mkString("\n"))
      idx <- promptForInt("Correct option number: ").map(_ - 1)
    yield MultipleChoiceData(options, idx)

  private def collectOptions(acc: List[String]): IO[List[String]] =
    promptForString(s"Option ${acc.length + 1}: ").flatMap: input =>
      if input.isBlank && acc.length >= 2 then IO.pure(acc)
      else if input.isBlank then IO.println("Need at least 2 options.") >> collectOptions(acc)
      else collectOptions(acc :+ input.trim)

  private def collectShortAnswerData: IO[QuestionData] =
    for limit <- promptForInt("Set a character limit for the answer: ")
    yield ShortAnswerData(limit)

  private def collectTrueFalseData: IO[QuestionData] =
    promptForString("Correct answer (true/false): ").flatMap: input =>
      input.trim.toLowerCase match
        case "true" => IO.pure(TrueFalseData(true))
        case "false" => IO.pure(TrueFalseData(false))
        case _ => IO.println("Please enter 'true' or 'false'.") >> collectTrueFalseData

  private def deleteQuestionFlow(examId: ExamId): IO[String] =
    client
      .getQuestions(examId, token)
      .flatMap:
        case Left(err) => IO.pure(s"Error: $err")
        case Right(questions) =>
          if questions.isEmpty then IO.pure("No questions to delete.")
          else selectAndDelete(examId, questions)

  private def selectAndDelete(examId: ExamId, questions: List[Question]): IO[String] =
    for
      _ <- clearConsole
      _ <- IO.println("=== Delete Question ===")
      _ <- IO.println(
        questions.zipWithIndex.map { case (q, i) => s"${i + 1}. ${q.questionText}" }.mkString("\n")
      )
      _ <- IO.println("") >> IO.println("0. Cancel")
      input <- promptForString("> ").map(_.trim)
      msg <- input.toIntOption match
        case Some(0) => IO.pure("")
        case Some(n) if n >= 1 && n <= questions.length =>
          client
            .deleteQuestion(examId, questions(n - 1).id, token)
            .map:
              case Left(err) => s"Error: $err"
              case Right(_) => "Question deleted."
        case _ => selectAndDelete(examId, questions)
    yield msg

  private def formatExamFormError(err: ExamFormError): String = err match
    case InvalidNameError(msg) => msg
    case InvalidDescriptionError(msg) => msg
    case InvalidTimeLimitError(msg) => msg

  private def formatQuestionFormError(err: QuestionFormError): String = err match
    case InvalidQuestionText(msg) => msg
    case InvalidPoints(msg) => msg
    case InvalidQuestionData(msg) => msg

  private def openExamFlow(examId: ExamId): IO[Unit] =
    for
      result <- client.openExam(examId, token)
      _ <- result.fold(
        {
          case ExamDoesNotExist(_) => IO.println("Exam not found.")
          case NotAnOwner(_, _) => IO.println("You don't own this exam.")
          case ExamStatusMismatch(_, msg) => IO.println(msg)
          case other => IO.println(s"Error: $other")
        },
        _ => IO.println("Exam opened successfully!")
      )
      _ <- pressEnterToContinue
    yield ()

  private def closeExamFlow(examId: ExamId): IO[Unit] =
    for
      result <- client.closeExam(examId, token)
      _ <- result.fold(
        {
          case ExamDoesNotExist(_) => IO.println("Exam not found.")
          case NotAnOwner(_, _) => IO.println("You don't own this exam.")
          case ExamStatusMismatch(_, msg) => IO.println(msg)
          case other => IO.println(s"Error: $other")
        },
        _ => IO.println("Exam closed successfully!")
      )
      _ <- pressEnterToContinue
    yield ()

  private def viewResultsFlow(examId: ExamId): IO[Unit] =
    for
      _ <- clearConsole
      result <- client.getResults(examId, token)
      _ <- result.fold(
        err => IO.println(s"Error: $err"),
        submissions =>
          if submissions.isEmpty then IO.println("No submissions yet.")
          else IO.println(displaySubmissionsList(submissions))
      )
      _ <- pressEnterToContinue
    yield ()

  private def displaySubmissionsList(submissions: List[Submission]): String =
    submissions.zipWithIndex
      .map { case (s, i) =>
        s"${i + 1}. Student ${s.studentId.value} [${s.status}] - ${s.score.fold("Not graded yet")(sc => s"$sc pts")}"
      }
      .mkString("\n")

  private def gradeShortAnswersFlow(examId: ExamId): IO[Unit] =
    for
      result <- client.getResults(examId, token)
      _ <- result.fold(
        err => IO.println(s"Error: $err") >> pressEnterToContinue,
        submissions =>
          if submissions.isEmpty then IO.println("No submissions yet.") >> pressEnterToContinue
          else selectSubmissionToGrade(examId, submissions)
      )
    yield ()

  private def selectSubmissionToGrade(examId: ExamId, submissions: List[Submission]): IO[Unit] =
    for
      _ <- clearConsole
      _ <- IO.println("=== Select Submission ===")
      _ <- IO.println(displaySubmissionsList(submissions))
      _ <- IO.println("") >> IO.println("0. Back")
      input <- promptForString("> ").map(_.trim)
      _ <- input.toIntOption match
        case Some(0) => ().pure[IO]
        case Some(n) if n >= 1 && n <= submissions.length =>
          selectQuestionToGrade(examId, submissions(n - 1)) >> selectSubmissionToGrade(examId, submissions)
        case _ => selectSubmissionToGrade(examId, submissions)
    yield ()

  private def selectQuestionToGrade(examId: ExamId, submission: Submission): IO[Unit] =
    for
      result <- client.getQuestions(examId, token)
      _ <- result.fold(
        err => IO.println(s"Error: $err") >> pressEnterToContinue,
        questions =>
          val shortAnswerQuestions = questions.filter(_.questionType == QuestionType.ShortAnswer)
          if shortAnswerQuestions.isEmpty then IO.println("No short-answer questions in this exam.") >> pressEnterToContinue
          else gradeQuestionMenu(examId, submission.id, shortAnswerQuestions)
      )
    yield ()

  private def gradeQuestionMenu(examId: ExamId, submissionId: SubmissionId, questions: List[Question]): IO[Unit] =
    for
      _ <- clearConsole
      _ <- IO.println("=== Select Question to Grade ===")
      _ <- IO.println(
        questions.zipWithIndex.map { case (q, i) => s"${i + 1}. ${q.questionText} (${q.points} pts)" }.mkString("\n")
      )
      _ <- IO.println("") >> IO.println("0. Back")
      input <- promptForString("> ").map(_.trim)
      _ <- input.toIntOption match
        case Some(0) => ().pure[IO]
        case Some(n) if n >= 1 && n <= questions.length =>
          gradeAnswerFlow(examId, submissionId, questions(n - 1)).flatMap(msg => IO.println(msg) >> pressEnterToContinue)
        case _ => gradeQuestionMenu(examId, submissionId, questions)
    yield ()

  private def gradeAnswerFlow(examId: ExamId, submissionId: SubmissionId, question: Question): IO[String] =
    for
      _ <- clearConsole
      answerResult <- client.getAnswer(examId, submissionId, question.id, token)
      msg <- answerResult match
        case Left(err) => IO.pure(s"Error: $err")
        case Right(answer) =>
          for
            _ <- IO.println(s"=== ${question.questionText} ===")
            _ <- IO.println(s"Answer: ${formatAnswerData(answer.data)}")
            points <- promptForDecimal(s"Points to award (0 - ${question.points}): ")
            result <- client.gradeAnswer(examId, submissionId, question.id, GradeAnswerForm(points), token)
          yield result.fold(
            err => s"Error: $err",
            _ => "Answer graded."
          )
    yield msg

  private def formatAnswerData(data: AnswerData): String = data match
    case MultipleChoice(idx) => s"Option ${idx + 1}"
    case TrueFalse(answer) => answer.map(_.toString).getOrElse("none")
    case ShortAnswer(answer) => answer
