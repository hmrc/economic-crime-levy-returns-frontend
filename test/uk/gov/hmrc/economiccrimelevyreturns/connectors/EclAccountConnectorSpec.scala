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
import uk.gov.hmrc.economiccrimelevyreturns.models.ObligationData
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.Future

class EclAccountConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new EclAccountConnector(appConfig, mockHttpClient)
  val eclAccountUrl              = "http://localhost:14009/economic-crime-levy-account"

  "getObligations" should {
    "return optional obligations when the http client returns optional obligations" in forAll {
      (
        obligationData: Option[ObligationData]
      ) =>
        val expectedUrl = s"$eclAccountUrl/obligation-data"

        when(
          mockHttpClient.GET[Option[ObligationData]](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            any()
          )(any(), any(), any())
        )
          .thenReturn(Future.successful(obligationData))

        val result = await(
          connector.getObligations()
        )

        result shouldBe obligationData

        verify(mockHttpClient, times(1))
          .GET[Option[ObligationData]](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            any()
          )(any(), any(), any())

        reset(mockHttpClient)
    }
  }

}
