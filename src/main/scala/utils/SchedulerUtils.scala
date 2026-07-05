package utils

import cats.effect.{IO, Resource}
import cats.implicits.*

import scala.concurrent.duration.FiniteDuration

import submission.SubmissionService

object SchedulerUtils:
  def scheduleAutoSubmissions(submissionService: SubmissionService, duration: FiniteDuration): Resource[IO, Unit] =
    val autoSubmitStream =
      fs2.Stream.awakeEvery[IO](duration).evalMap(
        _ => submissionService.autoSubmitPastDeadline.void
      )

    Resource.make(autoSubmitStream.compile.drain.start)
                  (_.cancel)
            .void
