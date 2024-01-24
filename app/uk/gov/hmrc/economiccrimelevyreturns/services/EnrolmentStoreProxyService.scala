/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.{EclEnrolment, Enrolment}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataHandlingError
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class EnrolmentStoreProxyService @Inject() (enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector)(implicit
  ec: ExecutionContext
) {

  def getEclRegistrationDate(
    eclRegistrationReference: String
  )(implicit hc: HeaderCarrier): EitherT[Future, DataHandlingError, LocalDate] =
    EitherT {
      enrolmentStoreProxyConnector
        .queryKnownFacts(eclRegistrationReference)
        .map { queryKnownFactsResponse =>
          val enrolment: Option[Enrolment] =
            queryKnownFactsResponse.enrolments.find(_.identifiers.exists(_.value == eclRegistrationReference))

          enrolment
            .flatMap(_.verifiers.find(_.key == EclEnrolment.VerifierKey))
            .map(eclRegistrationDate =>
              Right(LocalDate.parse(eclRegistrationDate.value, DateTimeFormatter.BASIC_ISO_DATE))
            )
            .getOrElse(
              Left(
                DataHandlingError
                  .InternalUnexpectedError(None, Some("ECL registration date could not be found in the enrolment"))
              )
            )
        }
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DataHandlingError.BadGateway(reason = message, code = code))
          case NonFatal(thr) => Left(DataHandlingError.InternalUnexpectedError(Some(thr)))
        }
    }

}
