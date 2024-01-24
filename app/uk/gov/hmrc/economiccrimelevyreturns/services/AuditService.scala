/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
