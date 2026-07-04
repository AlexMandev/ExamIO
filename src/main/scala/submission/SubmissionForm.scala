package submission

import io.circe.Codec
import sttp.tapir.Schema

import user.StudentId
import exam.ExamId

case class SubmissionForm(
  examId: ExamId,
  studentId: StudentId
) derives Codec,
      Schema
