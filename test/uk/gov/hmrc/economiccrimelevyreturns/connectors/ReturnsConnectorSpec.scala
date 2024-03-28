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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.{JsNull, Json}
import uk.gov.hmrc.economiccrimelevyreturns.ValidGetEclReturnSubmissionResponse
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn, SubmitEclReturnResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class ReturnsConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new ReturnsConnector(appConfig, mockHttpClient, config, actorSystem)
  val eclReturnsUrl                      = "http://localhost:14003/economic-crime-levy-returns"

  override def beforeEach(): Unit = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

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
      when(mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(eclReturn)))(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(NO_CONTENT, Json.stringify(JsNull))))

      await(connector.upsertReturn(eclReturn)) shouldBe ()

      reset(mockHttpClient)
    }

    "return UpstreamErrorResponse when call to returns service returns an error" in forAll { (eclReturn: EclReturn) =>
      val expectedUrl = url"$eclReturnsUrl/returns"

      val errorCode = INTERNAL_SERVER_ERROR

      when(mockHttpClient.put(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(eclReturn)))(any(), any(), any()))
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

  "validateEclReturn" should {
    "return None when the http client returns 200 OK and body is empty" in forAll { internalId: String =>
      val expectedUrl = url"$eclReturnsUrl/returns/$internalId/validation-errors"

      when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(JsNull))))

      val result = await(connector.validateEclReturn(internalId).value)

      result shouldBe None
    }

    "return Some with DataValidationError when 200 OK is returned with validation errors in the body" in forAll {
      (internalId: String, dataValidationError: String) =>
        val expectedUrl = url"$eclReturnsUrl/returns/$internalId/validation-errors"

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(dataValidationError)))))

        val result = await(connector.validateEclReturn(internalId).value)

        result shouldBe Some(dataValidationError)
    }

    "return 5xx UpstreamErrorResponse when call to returns service returns an error" in forAll { (internalId: String) =>
      val expectedUrl = url"$eclReturnsUrl/returns/$internalId/validation-errors"

      val errorCode = INTERNAL_SERVER_ERROR

      when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

      Try(await(connector.validateEclReturn(internalId).value)) match {
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
    }

    "return UpstreamErrorResponse when call to returns service returns an error" in forAll { (internalId: String) =>
      val expectedUrl = url"$eclReturnsUrl/submit-return/$internalId"
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

  "getEclReturnSubmission" should {

    val periodKey    = "22XY"
    val eclReference = "AAA"

    "return a Get ECL return submission response when the http client returns an Get ECL return submission response" in forAll {
      (
        validResponse: ValidGetEclReturnSubmissionResponse
      ) =>
        beforeEach()

        when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse.apply(OK, Json.stringify(Json.toJson(validResponse.response)))
            )
          )

        val result = await(connector.getEclReturnSubmission(periodKey, eclRegistrationReference))

        result shouldBe validResponse.response
    }

    "return UpstreamErrorResponse when call to returns service returns an error" in {
      beforeEach()

      val expectedUrl = url"$eclReturnsUrl/submission/$periodKey/$eclReference"
      val errorCode   = INTERNAL_SERVER_ERROR

      when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(errorCode, "INTERNAL_SERVER_ERROR")))

      Try(await(connector.getEclReturnSubmission(periodKey, eclReference))) match {
        case Failure(UpstreamErrorResponse(_, code, _, _)) =>
          code shouldEqual errorCode
        case _                                             => fail("expected UpstreamErrorResponse when an error is received from the returns service")
      }
    }

    "return calculated liability" in {
      beforeEach()

      val expectedUrl         = url"$eclReturnsUrl/calculate-liability"
      val calculatedLiability = random[CalculatedLiability]

      when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(calculatedLiability)))))

      val result = await(connector.calculateLiability(random[Int], random[Int], random[Long]))

      result shouldBe calculatedLiability

    }
  }
}
