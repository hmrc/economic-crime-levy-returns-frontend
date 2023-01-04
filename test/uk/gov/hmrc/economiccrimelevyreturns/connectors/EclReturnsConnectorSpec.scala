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
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.http.{HttpClient, HttpResponse}

import scala.concurrent.Future

class EclReturnsConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new EclReturnsConnector(appConfig, mockHttpClient)
  val eclReturnsUrl              = "http://localhost:14003/economic-crime-levy-returns/returns"

  "getReturn" should {
    "return an ecl return when the http client returns an ecl return" in forAll {
      (internalId: String, eclReturn: EclReturn) =>
        val expectedUrl = s"$eclReturnsUrl/$internalId"

        when(mockHttpClient.GET[Option[EclReturn]](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(eclReturn)))

        val result = await(connector.getReturn(internalId))
        result shouldBe Some(eclReturn)

        verify(mockHttpClient, times(1))
          .GET[Option[EclReturn]](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any())

        reset(mockHttpClient)
    }

    "return none when the http client returns none" in forAll { internalId: String =>
      val expectedUrl = s"$eclReturnsUrl/$internalId"

      when(mockHttpClient.GET[Option[EclReturn]](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = await(connector.getReturn(internalId))
      result shouldBe None

      verify(mockHttpClient, times(1))
        .GET[Option[EclReturn]](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any())

      reset(mockHttpClient)
    }
  }

  "deleteReturn" should {
    "return unit when the http client successfully returns a http response" in forAll { internalId: String =>
      val response    = HttpResponse(NO_CONTENT, "", Map.empty)
      val expectedUrl = s"$eclReturnsUrl/$internalId"

      when(mockHttpClient.DELETE[HttpResponse](ArgumentMatchers.eq(expectedUrl), any())(any(), any(), any()))
        .thenReturn(Future.successful(response))

      val result: Unit = await(connector.deleteReturn(internalId))
      result shouldBe ()

      verify(mockHttpClient, times(1))
        .DELETE[HttpResponse](ArgumentMatchers.eq(expectedUrl), any())(any(), any(), any())

      reset(mockHttpClient)
    }
  }

  "upsertReturn" should {
    "return the new or updated ecl return" in forAll { eclReturn: EclReturn =>
      val expectedUrl = eclReturnsUrl

      when(
        mockHttpClient
          .PUT[EclReturn, EclReturn](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any(), any())
      )
        .thenReturn(Future.successful(eclReturn))

      val result = await(connector.upsertReturn(eclReturn))
      result shouldBe eclReturn

      verify(mockHttpClient, times(1))
        .PUT[EclReturn, EclReturn](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any(), any())

      reset(mockHttpClient)
    }
  }
}
