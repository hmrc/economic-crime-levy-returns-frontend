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
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, SubmitEclReturnResponse}
import uk.gov.hmrc.http.{HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class ReturnsConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new ReturnsConnector(appConfig, mockHttpClient)
  val eclReturnsUrl              = "http://localhost:14003/economic-crime-levy-returns"

  "getReturn" should {
    "return an ecl return when the http client returns an ecl return" in forAll {
      (internalId: String, eclReturn: EclReturn) =>
        val expectedUrl = s"$eclReturnsUrl/returns/$internalId"

        when(mockHttpClient.GET[HttpResponse](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(eclReturn)))))

        val result = await(connector.getReturn(internalId))
        result shouldBe eclReturn

        verify(mockHttpClient, times(1))
          .GET[Option[EclReturn]](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any())

        reset(mockHttpClient)
    }

    "return none when the http client returns none" in forAll { internalId: String =>
      val expectedUrl = s"$eclReturnsUrl/returns/$internalId"

      when(mockHttpClient.GET[HttpResponse](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(NOT_FOUND, "Not found")))

      Try(await(connector.getReturn(internalId))) match {
        case Success(_) => fail("Expected UpstreamErrorResponse")
        case Failure(e) => e.isInstanceOf[UpstreamErrorResponse] shouldBe true
      }

      verify(mockHttpClient, times(1))
        .GET[Option[EclReturn]](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any())

      reset(mockHttpClient)
    }
  }

  "deleteReturn" should {
    "return unit when the http client successfully returns a http response" in forAll { internalId: String =>
      val response    = HttpResponse(NO_CONTENT, "", Map.empty)
      val expectedUrl = s"$eclReturnsUrl/returns/$internalId"

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
      val expectedUrl = s"$eclReturnsUrl/returns"

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

  "getReturnValidationErrors" should {
    "return None when the http client returns 200 OK" in forAll { internalId: String =>
      val expectedUrl = s"$eclReturnsUrl/returns/$internalId/validation-errors"

      val response = HttpResponse(OK, "")

      when(
        mockHttpClient
          .GET[HttpResponse](ArgumentMatchers.eq(expectedUrl), any(), any())(
            any(),
            any(),
            any()
          )
      ).thenReturn(Future.successful(response))

      val result = await(connector.getReturnValidationErrors(internalId))

      result shouldBe None
    }

    "return option of Unit when the http client return 400 bad request with validation errors" in forAll {
      (internalId: String, dataValidationErrors: DataValidationErrors) =>
        val expectedUrl = s"$eclReturnsUrl/returns/$internalId/validation-errors"

        val response = HttpResponse(BAD_REQUEST, Json.toJson(dataValidationErrors), Map.empty)

        when(
          mockHttpClient
            .GET[HttpResponse](ArgumentMatchers.eq(expectedUrl), any(), any())(
              any(),
              any(),
              any()
            )
        ).thenReturn(Future.successful(response))

        val result = await(connector.getReturnValidationErrors(internalId))

        result shouldBe Some(())
    }

    "throw a HttpException when an unexpected http status is returned by the http client" in forAll {
      internalId: String =>
        val expectedUrl = s"$eclReturnsUrl/returns/$internalId/validation-errors"

        val response = HttpResponse(INTERNAL_SERVER_ERROR, "")

        when(
          mockHttpClient
            .GET[HttpResponse](ArgumentMatchers.eq(expectedUrl), any(), any())(
              any(),
              any(),
              any()
            )
        ).thenReturn(Future.successful(response))

        val result: HttpException = intercept[HttpException] {
          await(connector.getReturnValidationErrors(internalId))
        }

        result.getMessage shouldBe s"Unexpected response with HTTP status $INTERNAL_SERVER_ERROR"
    }

    "throw an UpstreamErrorResponse exception when the http client returns a error response" in forAll {
      internalId: String =>
        val expectedUrl = s"$eclReturnsUrl/returns/$internalId/validation-errors"

        val response = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR)

        when(
          mockHttpClient
            .GET[HttpResponse](ArgumentMatchers.eq(expectedUrl), any(), any())(
              any(),
              any(),
              any()
            )
        ).thenReturn(Future.failed(response))

        val result: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
          await(connector.getReturnValidationErrors(internalId))
        }

        result.getMessage shouldBe "Internal server error"
    }
  }

  "submitReturn" should {
    "return an ECL return submission response when the http client returns an ECL return submission response" in forAll {
      (internalId: String, eclReturnResponse: SubmitEclReturnResponse) =>
        val expectedUrl = s"$eclReturnsUrl/submit-return/$internalId"

        when(
          mockHttpClient
            .POSTEmpty[SubmitEclReturnResponse](ArgumentMatchers.eq(expectedUrl), any())(any(), any(), any())
        ).thenReturn(Future.successful(eclReturnResponse))

        val result = await(connector.submitReturn(internalId))

        result shouldBe eclReturnResponse

        verify(mockHttpClient, times(1))
          .POSTEmpty[SubmitEclReturnResponse](
            ArgumentMatchers.eq(expectedUrl),
            any()
          )(any(), any(), any())

        reset(mockHttpClient)
    }
  }

}
