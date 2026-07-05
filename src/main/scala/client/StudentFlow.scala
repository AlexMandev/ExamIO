package client

import cats.effect.IO
import cats.syntax.all.*
import exam.{Exam, ExamDoesNotExist, ExamStatusMismatch}
import question.{
  PublicMultipleChoiceData,
  PublicQuestion,
  PublicQuestionData,
  PublicShortAnswerData,
  PublicTrueFalseData
}
import submission.{AlreadySubmitted, Submission, SubmissionAlreadyExists, SubmissionId, SubmissionNotInProgress, SubmissionStatus}
import answers.{
  AnswerData,
  AnswerDoesNotExist,
  AnswerForm,
  AnswerFormError,
  AnswerFormValidationError,
  InvalidOptionIndex,
  MultipleChoice,
  ShortAnswer,
  ShortAnswerExceedsLimit,
  TrueFalse
}
import CommonFlow.*
import client.utils.OptionUtils.*

class StudentFlow(client: ExamIOApiClient, token: String):
  def run(preliminaryMessage: Option[String] = None): IO[Unit] =
    recoverToMenu(
      for
        _ <- clearConsole
        _ <- preliminaryMessage.printLn
        _ <- displayMenu
        command <- promptForString("> ").map(_.trim)
        _ <- command match
          case "1" => browseOpenExamsFlow >> run()
          case "2" => myFinishedSubmissionsFlow >> run()
          case "3" => IO.println("Logged out.") >> pressEnterToContinue
          case _ => run()
      yield (),
      retry = run()
    )

  private def displayMenu: IO[Unit] =
    IO.println(
      """|=== Menu ===
         |1. Browse open exams
         |2. My finished exams
         |3. Logout
         |""".stripMargin
    )

  private def myFinishedSubmissionsFlow: IO[Unit] =
    for
      _ <- clearConsole
      result <- client.getMySubmissions(token)
      _ <- result.fold(
        err => IO.println(s"Error: $err"),
        submissions =>
          if submissions.isEmpty then IO.println("No finished exams yet.")
          else IO.println(displayFinishedSubmissionsList(submissions))
      )
      _ <- pressEnterToContinue
    yield ()

  private def displayFinishedSubmissionsList(submissions: List[Submission]): String =
    submissions.zipWithIndex
      .map { case (s, i) =>
        val gradeInfo = s.score.fold("Not graded yet")(score => s"Graded - $score pts")
        s"${i + 1}. $gradeInfo"
      }
      .mkString("\n")

  private def browseOpenExamsFlow: IO[Unit] =
    for
      result <- client.listOpenExams(token)
      _ <- result.fold(
        err => IO.println(s"Error: $err") >> pressEnterToContinue,
        exams =>
          if exams.isEmpty then IO.println("No open exams right now.") >> pressEnterToContinue
          else openExamListMenu(exams)
      )
    yield ()

  private def openExamListMenu(exams: List[Exam]): IO[Unit] =
    for
      _ <- clearConsole
      _ <- displayOpenExamList(exams)
      input <- promptForString("Select exam (0 to go back): ").map(_.trim)
      _ <- input.toIntOption match
        case Some(0) => ().pure[IO]
        case Some(n) if n >= 1 && n <= exams.length => startOrResumeSubmission(exams(n - 1)) >> openExamListMenu(exams)
        case _ => openExamListMenu(exams)
    yield ()

  private def displayOpenExamList(exams: List[Exam]): IO[Unit] =
    IO.println("=== Open Exams ===") >> IO.println(
      exams.zipWithIndex
        .map { case (exam, i) => s"${i + 1}. ${exam.name} (${exam.timeLimitMinutes} min)" }
        .mkString("\n")
    )

  private def startOrResumeSubmission(exam: Exam): IO[Unit] =
    client
      .createSubmission(exam.id, token)
      .flatMap:
        case Right(submission) => submissionMenu(exam, submission)
        case Left(SubmissionAlreadyExists(_, _, submissionId)) => resumeSubmission(exam, submissionId)
        case Left(ExamDoesNotExist(_)) => IO.println("Exam not found.") >> pressEnterToContinue
        case Left(ExamStatusMismatch(_, msg)) => IO.println(msg) >> pressEnterToContinue
        case Left(other) => IO.println(s"Error: $other") >> pressEnterToContinue

  private def resumeSubmission(exam: Exam, submissionId: SubmissionId): IO[Unit] =
    client
      .getResult(exam.id, submissionId, token)
      .flatMap:
        case Right(submission) => submissionMenu(exam, submission)
        case Left(err) => IO.println(s"Error: $err") >> pressEnterToContinue

  private def submissionMenu(exam: Exam, submission: Submission, preliminaryMessage: Option[String] = None): IO[Unit] =
    submission.status match
      case SubmissionStatus.InProgress => inProgressSubmissionMenu(exam, submission, preliminaryMessage)
      case _ => (clearConsole >> showResult(exam, submission)) >> pressEnterToContinue

  private def inProgressSubmissionMenu(exam: Exam, submission: Submission, preliminaryMessage: Option[String])
    : IO[Unit] =
    for
      _ <- clearConsole
      _ <- preliminaryMessage.printLn
      _ <- displaySubmissionMenu(exam)
      command <- promptForString("> ").map(_.trim)
      _ <- command match
        case "1" => viewQuestionsFlow(exam, submission) >> refreshSubmissionMenu(exam, submission)
        case "2" => finishSubmissionFlow(exam, submission)
        case "3" => ().pure[IO]
        case _ => submissionMenu(exam, submission)
    yield ()

  private def refreshSubmissionMenu(exam: Exam, submission: Submission): IO[Unit] =
    client.getResult(exam.id, submission.id, token).flatMap:
      case Right(refreshed) => submissionMenu(exam, refreshed)
      case Left(err) => IO.println(s"Error: $err") >> pressEnterToContinue

  private def displaySubmissionMenu(exam: Exam): IO[Unit] =
    IO.println(
      s"""|=== ${exam.name} ===
          |1. View / answer questions
          |2. Finish exam
          |3. Back
          |""".stripMargin
    )

  private def showResult(exam: Exam, submission: Submission): IO[Unit] =
    IO.println(
      s"""|=== ${exam.name} ===
          |Status: ${submission.status}
          |Score: ${submission.score.fold("Not graded yet")(_.toString)}
          |""".stripMargin
    )

  private def finishSubmissionFlow(exam: Exam, submission: Submission): IO[Unit] =
    for
      result <- client.finishSubmission(exam.id, submission.id, token)
      _ <- clearConsole
      _ <- result.fold(
        {
          case SubmissionNotInProgress(_, _) => IO.println("This submission has already been finished.")
          case ExamStatusMismatch(_, msg) => IO.println(msg)
          case other => IO.println(s"Error: $other")
        },
        finished => showResult(exam, finished)
      )
      _ <- pressEnterToContinue
    yield ()

  private def viewQuestionsFlow(exam: Exam, submission: Submission): IO[Unit] =
    for
      result <- client.getStudentQuestions(exam.id, submission.id, token)
      _ <- result.fold(
        err => IO.println(s"Error: $err") >> pressEnterToContinue,
        questions =>
          if questions.isEmpty then IO.println("No questions yet.") >> pressEnterToContinue
          else questionListMenu(exam, submission, questions)
      )
    yield ()

  private def questionListMenu(exam: Exam, submission: Submission, questions: List[PublicQuestion]): IO[Unit] =
    for
      _ <- clearConsole
      _ <- displayQuestionList(questions)
      input <- promptForString("Select question (0 to go back): ").map(_.trim)
      _ <- input.toIntOption match
        case Some(0) => ().pure[IO]
        case Some(n) if n >= 1 && n <= questions.length =>
          questionDetailFlow(exam, submission, questions(n - 1)) >> questionListMenu(exam, submission, questions)
        case _ => questionListMenu(exam, submission, questions)
    yield ()

  private def displayQuestionList(questions: List[PublicQuestion]): IO[Unit] =
    IO.println("=== Questions ===") >> IO.println(
      questions.zipWithIndex
        .map { case (q, i) => s"${i + 1}. [${q.questionType}] ${q.questionText} (${q.points} pts)" }
        .mkString("\n")
    )

  private def questionDetailFlow(
    exam: Exam,
    submission: Submission,
    question: PublicQuestion,
    preliminaryMessage: Option[String] = None
  ): IO[Unit] =
    for
      _ <- clearConsole
      _ <- preliminaryMessage.printLn
      _ <- displayQuestion(question)
      currentAnswer <- client.getAnswer(exam.id, submission.id, question.id, token)
      _ <- currentAnswer.fold(
        {
          case AnswerDoesNotExist(_, _) => IO.println("Current answer: none")
          case err => IO.println(s"Error: $err")
        },
        answer => IO.println(s"Current answer: ${formatAnswerData(answer.data)}")
      )
      _ <- displayQuestionDetailMenu
      command <- promptForString("> ").map(_.trim)
      _ <- command match
        case "1" =>
          answerQuestionFlow(exam, submission, question).flatMap(msg =>
            questionDetailFlow(exam, submission, question, Some(msg))
          )
        case "2" =>
          clearAnswerFlow(exam, submission, question).flatMap(msg =>
            questionDetailFlow(exam, submission, question, Some(msg))
          )
        case "3" => ().pure[IO]
        case _ => questionDetailFlow(exam, submission, question, preliminaryMessage)
    yield ()

  private def displayQuestionDetailMenu: IO[Unit] =
    IO.println(
      """|1. Answer / change answer
         |2. Clear answer
         |3. Back
         |""".stripMargin
    )

  private def displayQuestion(question: PublicQuestion): IO[Unit] =
    IO.println(s"=== ${question.questionText} (${question.points} pts) ===") >>
      (question.data match
        case PublicMultipleChoiceData(options) =>
          IO.println(options.zipWithIndex.map { case (opt, i) => s"  ${i + 1}. $opt" }.mkString("\n"))
        case PublicTrueFalseData() => IO.println("Answer: true / false")
        case PublicShortAnswerData(limit) => IO.println(s"Max length: $limit characters"))

  private def answerQuestionFlow(exam: Exam, submission: Submission, question: PublicQuestion): IO[String] =
    for
      data <- collectAnswerData(question.data)
      result <- client.addAnswer(exam.id, submission.id, question.id, AnswerForm(data), token)
    yield result.fold(
      {
        case AnswerFormValidationError(errs) =>
          "Validation errors:\n" + errs.toList.map(e => s"  - ${formatAnswerFormError(e)}").mkString("\n")
        case AlreadySubmitted(_, _, _) => "This exam has already been submitted — no more changes allowed."
        case other => s"Failed: $other"
      },
      _ => "Answer saved."
    )

  private def collectAnswerData(data: PublicQuestionData): IO[AnswerData] =
    data match
      case PublicMultipleChoiceData(options) =>
        for
          _ <- IO.println(options.zipWithIndex.map { case (opt, i) => s"  ${i + 1}. $opt" }.mkString("\n"))
          idx <- promptForInt("Your answer (option number): ").map(_ - 1)
        yield MultipleChoice(idx)
      case PublicTrueFalseData() =>
        promptForString("Your answer (true/false): ").flatMap: input =>
          input.trim.toLowerCase match
            case "true" => IO.pure(TrueFalse(Some(true)))
            case "false" => IO.pure(TrueFalse(Some(false)))
            case _ => IO.println("Please enter 'true' or 'false'.") >> collectAnswerData(data)
      case PublicShortAnswerData(limit) =>
        promptForString(s"Your answer (max $limit chars): ").map(ans => ShortAnswer(ans))

  private def clearAnswerFlow(exam: Exam, submission: Submission, question: PublicQuestion): IO[String] =
    client
      .clearAnswer(exam.id, submission.id, question.id, token)
      .map:
        case Left(AnswerDoesNotExist(_, _)) => "No answer to clear."
        case Left(err) => s"Error: $err"
        case Right(_) => "Answer cleared."

  private def formatAnswerData(data: AnswerData): String = data match
    case MultipleChoice(idx) => s"Option ${idx + 1}"
    case TrueFalse(answer) => answer.map(_.toString).getOrElse("none")
    case ShortAnswer(answer) => answer

  private def formatAnswerFormError(err: AnswerFormError): String = err match
    case InvalidOptionIndex(index) => s"Invalid option index: $index"
    case ShortAnswerExceedsLimit(limit) => s"Answer exceeds character limit of $limit"
