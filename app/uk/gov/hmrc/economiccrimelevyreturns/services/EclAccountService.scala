package uk.gov.hmrc.economiccrimelevyreturns.services

import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclAccountConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.ObligationData
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.EclAccountError
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class EclAccountService @Inject() (
  eclAccountConnector: EclAccountConnector
)(implicit ec: ExecutionContext) {

  def retrieveObligationData(implicit
    hc: HeaderCarrier
  ): EitherT[Future, EclAccountError, Option[ObligationData]] =
    EitherT {
      eclAccountConnector.getObligations().map(Right(_)).recover {
        case error @ UpstreamErrorResponse(message, code, _, _)
            if UpstreamErrorResponse.Upstream5xxResponse
              .unapply(error)
              .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
          Left(EclAccountError.BadGateway(reason = message, code = code))
        case NonFatal(thr) => Left(EclAccountError.InternalUnexpectedError(thr.getMessage, Some(thr)))
      }
    }

}
