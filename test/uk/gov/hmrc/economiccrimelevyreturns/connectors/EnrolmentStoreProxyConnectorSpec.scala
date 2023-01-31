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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.KeyValue
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.{EclEnrolment, QueryKnownFactsRequest, QueryKnownFactsResponse}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._

import scala.concurrent.Future

class EnrolmentStoreProxyConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new EnrolmentStoreProxyConnectorImpl(appConfig, mockHttpClient)
  val enrolmentStoreUrl: String  = s"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store"

  "queryKnownFacts" should {

    "return known facts when the http client returns known facts" in forAll {
      (eclRegistrationReference: String, queryKnownFactsResponse: QueryKnownFactsResponse) =>
        val expectedUrl                    = s"$enrolmentStoreUrl/enrolments"
        val expectedQueryKnownFactsRequest = QueryKnownFactsRequest(
          service = EclEnrolment.ServiceName,
          knownFacts = Seq(
            KeyValue(EclEnrolment.IdentifierKey, eclRegistrationReference)
          )
        )

        when(
          mockHttpClient
            .POST[QueryKnownFactsRequest, QueryKnownFactsResponse](
              ArgumentMatchers.eq(expectedUrl),
              ArgumentMatchers.eq(expectedQueryKnownFactsRequest),
              any()
            )(
              any(),
              any(),
              any(),
              any()
            )
        )
          .thenReturn(Future.successful(queryKnownFactsResponse))

        val result = await(connector.queryKnownFacts(eclRegistrationReference))

        result shouldBe queryKnownFactsResponse

        verify(mockHttpClient, times(1))
          .POST[QueryKnownFactsRequest, QueryKnownFactsResponse](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(expectedQueryKnownFactsRequest),
            any()
          )(
            any(),
            any(),
            any(),
            any()
          )

        reset(mockHttpClient)
    }
  }
}
