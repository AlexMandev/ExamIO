package exam

import infrastructure.auth.AuthenticationService

class ExamController(examService: ExamService, authenticationService: AuthenticationService):
  import authenticationService.*

  def createExam = ExamEndpoints.createExamEndpoint.authenticate.serverLogic { user => form =>
    examService.createExam(form, user.id)
  }

  val endpoints = List()
