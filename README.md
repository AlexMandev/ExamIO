# examio

ExamIO is a RESTful backend API and CLI client for managing online examinations that supports
teacher and student roles, timed submissions, automatic submission on timeout,
and submission grading.

## features

- jwt-based authentication for student and teacher roles
- role-based access control for all operations
- automatic submissions after exam time limit has elapsed
- various question types: multiple choice, true/false, short answer
- automatic grading of closed-ended questions (or true/false)
- manual grading of open-ended questions
