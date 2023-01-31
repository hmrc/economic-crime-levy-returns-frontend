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

package uk.gov.hmrc.economiccrimelevyreturns.connectors

import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.KeyValue
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.{EclEnrolment, QueryKnownFactsRequest, QueryKnownFactsResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait EnrolmentStoreProxyConnector {
  def queryKnownFacts(eclRegistrationReference: String)(implicit hc: HeaderCarrier): Future[QueryKnownFactsResponse]
}

class EnrolmentStoreProxyConnectorImpl @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit
  ec: ExecutionContext
) extends EnrolmentStoreProxyConnector {

  private val enrolmentStoreUrl: String = s"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store"

  def queryKnownFacts(eclRegistrationReference: String)(implicit hc: HeaderCarrier): Future[QueryKnownFactsResponse] =
    httpClient.POST[QueryKnownFactsRequest, QueryKnownFactsResponse](
      s"$enrolmentStoreUrl/enrolments",
      QueryKnownFactsRequest(
        EclEnrolment.ServiceName,
        knownFacts = Seq(
          KeyValue(key = EclEnrolment.IdentifierKey, value = eclRegistrationReference)
        )
      )
    )

}
