package exam

import infrastructure.auth.AuthenticationService

import cats.effect.IO
import cats.syntax.all.*

import grading.GradingService
import user.TeacherId

class ExamController(examService: ExamService, gradingService: GradingService, authenticationService: AuthenticationService):
  import authenticationService.*

  def createExam = ExamEndpoints.createExamEndpoint.authenticate.serverLogic { user => form =>
    examService.createExam(form, TeacherId(user.id))
  }

  def getOwnExams = ExamEndpoints.getOwnExamsEndpoint.authenticate.serverLogic { user => _ =>
    examService.getExamsBy(TeacherId(user.id)).map(_.asRight)
  }

  def openExam = ExamEndpoints.openExamEndpoint.authenticate.serverLogic { user => examId =>
    examService.openExam(examId, TeacherId(user.id))
  }

  def closeExam = ExamEndpoints.closeExamEndpoint.authenticate.serverLogic { user => examId =>
    for
      result <- examService.closeExam(examId, TeacherId(user.id))
      _ <- result match
        case Right(_) => gradingService.gradeExamAutomatically(examId)
        case Left(_) => IO.unit
    yield result
  }

  val endpoints = List(createExam, getOwnExams, openExam, closeExam)
