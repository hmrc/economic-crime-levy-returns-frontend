package uk.gov.hmrc.economiccrimelevyreturns.models.errors


trait EmailSubmissionError

object EmailSubmissionError {
  case class InternalUnexpectedError(cause: Option[Throwable], message: Option[String] = None) extends EmailSubmissionError
  case class BadGateway(reason: String, code: Int) extends EmailSubmissionError
}
