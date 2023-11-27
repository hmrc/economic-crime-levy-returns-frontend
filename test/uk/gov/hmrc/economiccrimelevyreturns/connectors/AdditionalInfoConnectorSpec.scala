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
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyreturns.models.{AdditionalInfo, EclReturn, SubmitEclReturnResponse}
import uk.gov.hmrc.http.{HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class AdditionalInfoConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new AdditionalInfoConnector(appConfig, mockHttpClient)
  val eclReturnsUrl              = "http://localhost:14003/economic-crime-levy-returns"

  "getInfo" should {
    "return an additional info when the http client returns an ecl return" in forAll {
      (internalId: String, info: AdditionalInfo) =>
        val expectedUrl = s"$eclReturnsUrl/info/$internalId"

        when(mockHttpClient.GET[HttpResponse](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(info)))))

        val result = await(connector.getAdditionalInfo(internalId))
        result shouldBe info

        verify(mockHttpClient, times(1))
          .GET[Option[AdditionalInfo]](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any())

        reset(mockHttpClient)
    }

    "return none when the http client returns none" in forAll { internalId: String =>
      val expectedUrl = s"$eclReturnsUrl/info/$internalId"

      when(mockHttpClient.GET[HttpResponse](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(NOT_FOUND, "Not found")))

      Try(await(connector.getAdditionalInfo(internalId))) match {
        case Success(_) => fail("Expected UpstreamErrorResponse")
        case Failure(e) => e.isInstanceOf[UpstreamErrorResponse] shouldBe true
      }

      verify(mockHttpClient, times(1))
        .GET[Option[EclReturn]](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any())

      reset(mockHttpClient)
    }
  }

  "deleteAdditionalInfo" should {
    "return unit when the http client successfully returns a http response" in forAll { internalId: String =>
      val response    = HttpResponse(NO_CONTENT, "", Map.empty)
      val expectedUrl = s"$eclReturnsUrl/info/$internalId"

      when(mockHttpClient.DELETE[HttpResponse](ArgumentMatchers.eq(expectedUrl), any())(any(), any(), any()))
        .thenReturn(Future.successful(response))

      val result: Unit = await(connector.deleteAdditionalInfo(internalId))
      result shouldBe ()

      verify(mockHttpClient, times(1))
        .DELETE[HttpResponse](ArgumentMatchers.eq(expectedUrl), any())(any(), any(), any())

      reset(mockHttpClient)
    }
  }

  "upsertAdditionalInfo" should {
    "return the new or updated additional info" in forAll { info: AdditionalInfo =>
      val expectedUrl = s"$eclReturnsUrl/info"

      when(
        mockHttpClient.PUT[AdditionalInfo, AdditionalInfo](ArgumentMatchers.eq(expectedUrl), any(), any())(
          any(),
          any(),
          any(),
          any()
        )
      )
        .thenReturn(Future.successful(info))

      val result = await(connector.upsertAdditionalInfo(info))
      result shouldBe info

      verify(mockHttpClient, times(1))
        .PUT[AdditionalInfo, AdditionalInfo](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any(), any())

      reset(mockHttpClient)
    }
  }

}
