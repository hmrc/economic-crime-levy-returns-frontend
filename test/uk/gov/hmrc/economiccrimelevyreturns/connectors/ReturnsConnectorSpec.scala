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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, SubmitEclReturnResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class ReturnsConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new ReturnsConnector(appConfig, mockHttpClient, config, actorSystem)
  val eclReturnsUrl                      = "http://localhost:14003/economic-crime-levy-returns"

  "getReturn" should {
    "return an ecl return when the http client returns an ecl return" in forAll {
      (internalId: String, eclReturn: EclReturn) =>
        val expectedUrl = url"$eclReturnsUrl/returns/$internalId"

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(eclReturn)))))

        val result = await(connector.getReturn(internalId))
        result shouldBe eclReturn

        verify(mockHttpClient, times(1)).get(ArgumentMatchers.eq(expectedUrl))(any())

        reset(mockHttpClient)
    }

    "return UpstreamErrorResponse when call to returns service returns an error" in forAll { (internalId: String) =>
      val expectedUrl = url"$eclReturnsUrl/returns/$internalId"

      val errorCode = INTERNAL_SERVER_ERROR

      when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

      Try(await(connector.getReturn(internalId))) match {
        case Failure(UpstreamErrorResponse(_, code, _, _)) =>
          code shouldEqual errorCode
        case _                                             => fail("expected UpstreamErrorResponse when an error is received from the returns service")
      }
    }
  }

  "deleteReturn" should {
    "return unit when the http client successfully returns a http response" in forAll { internalId: String =>
      val expectedUrl = url"$eclReturnsUrl/returns/$internalId"

      when(mockHttpClient.delete(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(NO_CONTENT, "")))

      val result: Unit = await(connector.deleteReturn(internalId))
      result shouldBe ()

      verify(mockHttpClient, times(1)).delete(ArgumentMatchers.eq(expectedUrl))(any())

      reset(mockHttpClient)
    }

    "return UpstreamErrorResponse when call to returns service returns an error" in forAll { (internalId: String) =>
      val expectedUrl = url"$eclReturnsUrl/returns/$internalId"

      val errorCode = INTERNAL_SERVER_ERROR

      when(mockHttpClient.delete(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

      Try(await(connector.deleteReturn(internalId))) match {
        case Failure(UpstreamErrorResponse(_, code, _, _)) =>
          code shouldEqual errorCode
        case _                                             => fail("expected UpstreamErrorResponse when an error is received from the returns service")
      }
    }
  }

  "upsertReturn" should {
    "return the new or updated ecl return" in forAll { eclReturn: EclReturn =>
      val expectedUrl = url"$eclReturnsUrl/returns"

      when(mockHttpClient.put(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(ArgumentMatchers.eq(eclReturn))(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(eclReturn)))))

      val result = await(connector.upsertReturn(eclReturn))
      result shouldBe eclReturn

      reset(mockHttpClient)
    }

    "return UpstreamErrorResponse when call to returns service returns an error" in forAll {
      (internalId: String, eclReturn: EclReturn) =>
        val expectedUrl = url"$eclReturnsUrl/returns/$internalId"

        val errorCode = INTERNAL_SERVER_ERROR

        when(mockHttpClient.put(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(ArgumentMatchers.eq(eclReturn))(any(), any(), any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

        Try(await(connector.upsertReturn(eclReturn))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual errorCode
          case _                                             => fail("expected UpstreamErrorResponse when an error is received from the returns service")
        }
    }
  }

  "getReturnValidationErrors" should {
    "return None when the http client returns 200 OK and body is empty" in forAll { internalId: String =>
      val expectedUrl = url"$eclReturnsUrl/returns/$internalId/validation-errors"

      when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, "")))

      val result = await(connector.getReturnValidationErrors(internalId))

      result shouldBe None
    }

    "return Some with DataValidationErrors when 200 OK is returned with validation errors in the body" in forAll {
      (internalId: String, dataValidationErrors: DataValidationErrors) =>
        val expectedUrl = url"$eclReturnsUrl/returns/$internalId/validation-errors"

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(dataValidationErrors)))))

        val result = await(connector.getReturnValidationErrors(internalId))

        result shouldBe Some(dataValidationErrors)
    }

    "return 5xx UpstreamErrorResponse when call to returns service returns an error" in forAll { (internalId: String) =>
      val expectedUrl = url"$eclReturnsUrl/returns/$internalId"

      val errorCode = INTERNAL_SERVER_ERROR

      when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

      Try(await(connector.getReturnValidationErrors(internalId))) match {
        case Failure(UpstreamErrorResponse(_, code, _, _)) =>
          code shouldEqual errorCode
        case _                                             => fail("expected UpstreamErrorResponse when an error is received from the returns service")
      }
    }
  }

  "submitReturn" should {
    "return an ECL return submission response when the http client returns an ECL return submission response" in forAll {
      (internalId: String, eclReturnResponse: SubmitEclReturnResponse) =>
        val expectedUrl = url"$eclReturnsUrl/submit-return/$internalId"

        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(eclReturnResponse)))))

        val result = await(connector.submitReturn(internalId))

        result shouldBe eclReturnResponse

        verify(mockHttpClient, times(1)).post(ArgumentMatchers.eq(expectedUrl))

        reset(mockHttpClient)
    }

    "return UpstreamErrorResponse when call to returns service returns an error" in forAll { (internalId: String) =>
      val expectedUrl = url"$eclReturnsUrl/returns/$internalId"
      val errorCode   = INTERNAL_SERVER_ERROR

      when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

      Try(await(connector.submitReturn(internalId))) match {
        case Failure(UpstreamErrorResponse(_, code, _, _)) =>
          code shouldEqual errorCode
        case _                                             => fail("expected UpstreamErrorResponse when an error is received from the returns service")
      }
    }
  }

}
