package client

import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import user.{LoginResponse, UserRole}
import AuthFlow.{loginFlow, registerFlow}

import scala.util.Try

object CommonFlow:
  def startApp(client: ExamIOApiClient): IO[Unit] = greet >> mainLoop(client, false)

  def mainLoop(client: ExamIOApiClient, clearTerminal: Boolean): IO[Unit] =
    for
      _ <- if clearTerminal then clearConsole else IO.pure(())
      _ <- displayMenu
      command <- promptForStringLine("Choose an option: ").map(_.trim)
      _ <- command match
        case "1" => startTeacherFlow(client) >> mainLoop(client, true)
        case "2" => startStudentFlow(client) >> mainLoop(client, true)
        case "3" => registerFlow(client) >> mainLoop(client, true)
        case "4" => ().pure[IO]
        case _ => IO.println("Invalid option.") >> mainLoop(client, true)
    yield ()

  private def startTeacherFlow(client: ExamIOApiClient): IO[Unit] =
    loginFlow(client).flatMap:
      case Some(LoginResponse(token, UserRole.TEACHER)) => TeacherFlow(client, token).run()
      case Some(_) => IO.println("This is not a teacher account.") >> pressEnterToContinue >> mainLoop(client, true)
      case None => pressEnterToContinue >> mainLoop(client, true)

  private def startStudentFlow(client: ExamIOApiClient): IO[Unit] =
    loginFlow(client).flatMap:
      case Some(LoginResponse(token, UserRole.STUDENT)) => StudentFlow(client, token).run
      case Some(_) => IO.println("This is not a student account.") >> pressEnterToContinue >> mainLoop(client, true)
      case None => pressEnterToContinue >> mainLoop(client, true)

  def promptForString(prompt: String): IO[String] =
    IO.print(prompt) >> IO.readLine

  def promptForStringLine(prompt: String): IO[String] =
    IO.println(prompt) >> IO.readLine

  def pressEnterToContinue: IO[Unit] =
    IO.println("") >> IO.print("Press Enter to continue...") >> IO.readLine.void

  def promptForInt(prompt: String): IO[Int] =
    promptForString(prompt).flatMap: input =>
      input.trim.toIntOption match
        case Some(n) => IO.pure(n)
        case None => IO.println("Please enter a valid number.") >> promptForInt(prompt)

  def promptForDecimal(prompt: String): IO[BigDecimal] =
    promptForString(prompt).flatMap: input =>
      Try(BigDecimal(input.trim)).toOption match
        case Some(n) if n.scale <= 2 => IO.pure(n)
        case Some(_) => IO.println("Max 2 decimal places allowed.") >> promptForDecimal(prompt)
        case None => IO.println("Please enter a valid number.") >> promptForDecimal(prompt)

  def clearConsole: IO[Unit] = IO.print("\u001b[H\u001b[2J")

  private def greet: IO[Unit] =
    IO.println("""
                 | === ExamIO ===""".stripMargin)

  private def displayMenu: IO[Unit] =
    IO.println(
      """1. Login as Teacher
        |2. Login as Student
        |3. Register
        |4. Quit
        |""".stripMargin
    )
