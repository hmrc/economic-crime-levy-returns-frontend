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
      eclAccountConnector.getObligations().value.map(Right(_)).recover {
        case error @ UpstreamErrorResponse(message, code, _, _)
            if UpstreamErrorResponse.Upstream5xxResponse
              .unapply(error)
              .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
          Left(EclAccountError.BadGateway(reason = s"Get Obligation Data Failed - $message", code = code))
        case NonFatal(thr) => Left(EclAccountError.InternalUnexpectedError(Some(thr), Some(thr.getMessage)))
      }
    }

}
