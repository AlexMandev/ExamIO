package client

import cats.effect.IO
import cats.syntax.all.*
import exam.{
  Exam,
  ExamCannotBeClosed,
  ExamCannotBeOpened,
  ExamDoesNotExist,
  ExamForm,
  ExamFormError,
  ExamFormValidationError,
  ExamId,
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
  ShortAnswerData,
  TrueFalseData
}
import CommonFlow.*
import client.utils.OptionUtils.*

class TeacherFlow(client: ExamIOApiClient, token: String):
  def run(preliminaryMessage: Option[String] = None): IO[Unit] =
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
    yield ()

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
        case "6" => ().pure[IO]
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
          |6. Back
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
        case "3" => IO.pure(ShortAnswerData())
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
          case ExamCannotBeOpened(_, s) => IO.println(s"Cannot open: exam is $s.")
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
          case ExamCannotBeClosed(_, s) => IO.println(s"Cannot close: exam is $s.")
          case other => IO.println(s"Error: $other")
        },
        _ => IO.println("Exam closed successfully!")
      )
      _ <- pressEnterToContinue
    yield ()
