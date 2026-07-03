package client

import cats.effect.IO

def promptForString(prompt: String): IO[String] =
  IO.print(prompt) >> IO.readLine

def promptForStringLine(prompt: String): IO[String] =
  IO.println(prompt) >> IO.readLine

def greetAndDisplayMenu: IO[Unit] =
  IO.println(
    """|
       |=== ExamIO ===
       |1. Login as Teacher
       |2. Login as Student
       |3. Register
       |4. Quit
       |""".stripMargin
  )
