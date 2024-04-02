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

package uk.gov.hmrc.economiccrimelevyreturns.testonly.connectors.stubs

import uk.gov.hmrc.economiccrimelevyreturns.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.KeyValue
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.{EclEnrolment, Enrolment, QueryKnownFactsResponse}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.Future

class StubEnrolmentStoreProxyConnector @Inject() extends EnrolmentStoreProxyConnector {

  def queryKnownFacts(eclRegistrationReference: String)(implicit hc: HeaderCarrier): Future[QueryKnownFactsResponse] =
    Future.successful(
      QueryKnownFactsResponse(
        service = EclEnrolment.serviceName,
        enrolments = Seq(
          Enrolment(
            identifiers = Seq(KeyValue(EclEnrolment.identifierKey, eclRegistrationReference)),
            verifiers = Seq(KeyValue(EclEnrolment.verifierKey, "20230901"))
          )
        )
      )
    )

}
