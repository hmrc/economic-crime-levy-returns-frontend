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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculateLiabilityRequest, CalculatedLiability}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import scala.util.{Failure, Try}

import scala.concurrent.Future

class EclCalculatorConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new EclCalculatorConnector(appConfig, mockHttpClient, config, actorSystem)
  val eclCalculatorUrl                   = "http://localhost:14010/economic-crime-levy-calculator"
  val expectedUrl                        = url"$eclCalculatorUrl/calculate-liability"

  "calculateLiability" should {
    "return the calculated liability when the http client returns the calculated liability" in forAll {
      (calculateLiabilityRequest: CalculateLiabilityRequest, calculatedLiability: CalculatedLiability) =>
        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(calculatedLiability)))))

        val result = await(
          connector.calculateLiability(
            calculateLiabilityRequest.amlRegulatedActivityLength,
            calculateLiabilityRequest.relevantApLength,
            calculateLiabilityRequest.ukRevenue,
            calculateLiabilityRequest.year
          )
        )

        result shouldBe calculatedLiability

        reset(mockHttpClient)
        reset(mockHttpClient)
    }

    "return 5xx UpstreamErrorResponse when call to calculator service returns an error and executes retry" in {
      (calculateLiabilityRequest: CalculateLiabilityRequest) =>
        val errorCode = INTERNAL_SERVER_ERROR

        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(ArgumentMatchers.eq(calculateLiabilityRequest))(any(), any(), any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

        Try(await(connector.calculateLiability(any(), any(), any(), any()))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual errorCode
          case _                                             => fail("expected UpstreamErrorResponse when an error is received from the account service")
        }

        verify(mockRequestBuilder, times(4)).execute(any(), any())
    }
  }

}
