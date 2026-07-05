package submission

import cats.effect.IO
import sttp.tapir.server.ServerEndpoint
import infrastructure.auth.AuthenticationService

import user.StudentId
import user.TeacherId

class SubmissionController(submissionService: SubmissionService, authenticationService: AuthenticationService):
  import authenticationService.*

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

  val endpoints: List[ServerEndpoint[Any, IO]] = List(createSubmission, finishSubmission)
