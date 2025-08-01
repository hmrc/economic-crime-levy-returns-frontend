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

package uk.gov.hmrc.economiccrimelevyreturns.testonly.connectors

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.OK
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}

import scala.concurrent.Future

class TestOnlyConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new TestOnlyConnector(
    appConfig,
    mockHttpClient
  )

  val baseUrl                = s"${appConfig.eclReturnsBaseUrl}/economic-crime-levy-returns/test-only"
  val response: HttpResponse = HttpResponse(OK, "")

  override def beforeEach(): Unit = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  "clearAllData" should {
    "return as expected" in {
      val expectedUrl =
        url"$baseUrl/clear-all"

      when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, "")))

      val result = await(connector.clearAllData())
      result.status shouldBe response.status
    }
  }

  "clearCurrentData" should {
    "return as expected" in {
      val expectedUrl = url"$baseUrl/clear-current"

      when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, "")))

      val result = await(connector.clearCurrentData())
      result.status shouldBe response.status
    }
  }
}
