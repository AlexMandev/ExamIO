package submission

import cats.effect.{IO, Resource}
import infrastructure.auth.AuthenticationService
import sttp.tapir.server.ServerEndpoint
import infrastructure.db.DBDoobie.DBTransactor
import exam.ExamService

case class SubmissionModule(
  submissionRepository: SubmissionRepository,
  submissionService: SubmissionService,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object SubmissionModule:
  def apply(dbTransactor: DBTransactor, examService: ExamService, authenticationService: AuthenticationService)
    : Resource[IO, SubmissionModule] =
    val submissionRepository = SubmissionRepository(dbTransactor)
    val submissionService = SubmissionService(submissionRepository, examService)
    val submissionController = SubmissionController(submissionService, authenticationService)

    Resource.pure(SubmissionModule(submissionRepository, submissionService, submissionController.endpoints))
