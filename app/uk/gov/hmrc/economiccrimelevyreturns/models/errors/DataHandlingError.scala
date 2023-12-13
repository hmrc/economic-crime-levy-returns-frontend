package uk.gov.hmrc.economiccrimelevyreturns.models.errors
trait DataHandlingError

object DataHandlingError {
  case class InternalUnexpectedError(cause: Option[Throwable]) extends DataHandlingError
  case class BadGateway(reason: String, code: Int) extends DataHandlingError
}
