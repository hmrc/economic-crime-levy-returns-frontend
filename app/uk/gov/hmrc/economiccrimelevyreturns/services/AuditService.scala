package uk.gov.hmrc.economiccrimelevyreturns.services

import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyreturns.models.{FirstTimeReturn, ReturnType}
import uk.gov.hmrc.economiccrimelevyreturns.models.audit.ReturnStartedEvent
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.AuditError
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class AuditService @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext) {

  def auditReturnStarted(
    internalId: String,
    eclRegistrationReference: String,
    returnType: Option[ReturnType]
  )(implicit
    hc: HeaderCarrier
  ): EitherT[Future, AuditError, Unit] =
    EitherT {
      auditConnector
        .sendExtendedEvent(
          ReturnStartedEvent(
            internalId,
            eclRegistrationReference,
            returnType = returnType.getOrElse(FirstTimeReturn)
          ).extendedDataEvent
        )
        .map(_ => Right(()))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(AuditError.BadGateway(reason = message, code = code))
          case NonFatal(thr) => Left(AuditError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

}
