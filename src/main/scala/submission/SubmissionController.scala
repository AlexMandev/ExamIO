package submission

import cats.effect.IO
import cats.syntax.all.*
import sttp.tapir.server.ServerEndpoint
import infrastructure.auth.AuthenticationService

import user.StudentId
import user.TeacherId

class SubmissionController(submissionService: SubmissionService, authenticationService: AuthenticationService):
  import authenticationService.*

  def getMySubmissions = SubmissionEndpoints.getMySubmissionsEndpoint.authenticate.serverLogic { user => _ =>
    submissionService.getFinishedSubmissions(StudentId(user.id)).map(_.asRight)
  }

  def createSubmission = SubmissionEndpoints.createSubmissionEndpoint.authenticate.serverLogic { user => examId =>
    submissionService.createSubmission(SubmissionForm(examId, StudentId(user.id)))
  }

  def finishSubmission = SubmissionEndpoints.finishSubmissionEndpoint.authenticate.serverLogic {
    user => (examId, submissionId) =>
      submissionService.finishSubmission(StudentId(user.id), submissionId, examId)
  }

  def getResults = SubmissionEndpoints.getResultsEndpoint.authenticate.serverLogic {
    user => examId => submissionService.getResults(examId, TeacherId(user.id))
  }

  def getResult = SubmissionEndpoints.getResultEndpoint.authenticate.serverLogic {
    user => (examId, submissionId) => submissionService.getResult(examId, submissionId, StudentId(user.id))
  }

  val endpoints: List[ServerEndpoint[Any, IO]] =
    List(getMySubmissions, createSubmission, finishSubmission, getResults, getResult)
