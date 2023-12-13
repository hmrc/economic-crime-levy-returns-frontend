package uk.gov.hmrc.economiccrimelevyreturns.models.errors

trait LiabilityCalculationError

object LiabilityCalculationError {
  case class InternalUnexpectedError(cause: Option[Throwable]) extends LiabilityCalculationError
  case class BadGateway(reason: String, code: Int) extends LiabilityCalculationError
}
