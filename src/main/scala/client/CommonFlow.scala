package client

import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import user.{LoginResponse, UserRole}
import AuthFlow.{registerFlow, loginFlow}

object CommonFlow:
  def startApp(client: ExamIOApiClient): IO[Unit] = greet >> mainLoop(client, false)

  def mainLoop(client: ExamIOApiClient, clearTerminal: Boolean): IO[Unit] =
    for
      _ <- if clearTerminal then clearConsole else IO.pure(())
      _ <- displayMenu
      command <- promptForStringLine("Choose an option: ").map(_.trim)
      _ <- command match
        case "1" => startTeacherFlow(client)
        case "2" => startStudentFlow(client)
        case "3" => registerFlow(client) >> mainLoop(client, true)
        case "4" => ().pure[IO]
        case _ => IO.println("Invalid option.") >> mainLoop(client, true)
    yield ()

  private def startTeacherFlow(client: ExamIOApiClient): IO[Unit] =
    loginFlow(client).flatMap:
      case Some(LoginResponse(token, UserRole.TEACHER)) => TeacherFlow(client, token).run
      case Some(_) => IO.println("This is not a teacher account.") >> mainLoop(client, true)
      case None => mainLoop(client, true)

  private def startStudentFlow(client: ExamIOApiClient): IO[Unit] =
    loginFlow(client).flatMap:
      case Some(LoginResponse(token, UserRole.STUDENT)) => StudentFlow(client, token).run
      case Some(_) => IO.println("This is not a student account.") >> mainLoop(client, true)
      case None => mainLoop(client, true)

  def promptForString(prompt: String): IO[String] =
    IO.print(prompt) >> IO.readLine

  def promptForStringLine(prompt: String): IO[String] =
    IO.println(prompt) >> IO.readLine

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
