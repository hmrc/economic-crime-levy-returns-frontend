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

import uk.gov.hmrc.economiccrimelevyreturns.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.{EclEnrolment, Enrolment}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreProxyService @Inject() (enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector)(implicit
  ec: ExecutionContext
) {

  // TODO This method could return a "maybe" registration date (in the future).
  //      No registration date would mean the user is not yet registered
  //
  def getEclRegistrationDate(eclRegistrationReference: String)(implicit hc: HeaderCarrier): Future[LocalDate] =
    enrolmentStoreProxyConnector.queryKnownFacts(eclRegistrationReference).map { queryKnownFactsResponse =>
      val enrolment: Option[Enrolment] =
        queryKnownFactsResponse.enrolments.find(_.identifiers.exists(_.value == eclRegistrationReference))

      val eclRegistrationDateString: String =
        enrolment
          .flatMap(_.verifiers.find(_.key == EclEnrolment.VerifierKey))
          .map(_.value)
          .getOrElse(throw new IllegalStateException("ECL registration date could not be found in the enrolment"))

      LocalDate.parse(eclRegistrationDateString, DateTimeFormatter.BASIC_ISO_DATE)
    }

}
