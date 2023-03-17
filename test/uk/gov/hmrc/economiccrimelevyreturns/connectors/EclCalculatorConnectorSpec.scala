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
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculateLiabilityRequest, CalculatedLiability}
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.Future

class EclCalculatorConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new EclCalculatorConnector(appConfig, mockHttpClient)
  val eclCalculatorUrl           = "http://localhost:14010/economic-crime-levy-calculator"

  "calculateLiability" should {
    "return the calculated liability when the http client returns the calculated liability" in forAll {
      (
        calculateLiabilityRequest: CalculateLiabilityRequest,
        calculatedLiability: CalculatedLiability
      ) =>
        val expectedUrl = s"$eclCalculatorUrl/calculate-liability"

        when(
          mockHttpClient.POST[CalculateLiabilityRequest, CalculatedLiability](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(calculateLiabilityRequest),
            any()
          )(any(), any(), any(), any())
        )
          .thenReturn(Future.successful(calculatedLiability))

        val result = await(
          connector.calculateLiability(
            calculateLiabilityRequest.amlRegulatedActivityLength,
            calculateLiabilityRequest.relevantApLength,
            calculateLiabilityRequest.ukRevenue
          )
        )

        result shouldBe calculatedLiability

        verify(mockHttpClient, times(1))
          .POST[CalculateLiabilityRequest, CalculatedLiability](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(calculateLiabilityRequest),
            any()
          )(any(), any(), any(), any())

        reset(mockHttpClient)
    }
  }

}
