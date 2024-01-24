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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionData
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class SessionDataConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new SessionDataConnector(appConfig, mockHttpClient, config, actorSystem)
  val eclSessionDataUrl                  = "http://localhost:14003/economic-crime-levy-returns"

  override def beforeEach() = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  "get" should {

    "return SessionData data when request succeeds" in forAll { (internalId: String, sessionData: SessionData) =>
      val expectedUrl = url"$eclSessionDataUrl/session/$internalId"

      when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(sessionData)))))

      val result = await(connector.get(internalId))

      result shouldBe sessionData
    }

  }

  "delete" should {
    "return unit when the http client successfully returns a http response" in forAll { internalId: String =>
      val expectedUrl = url"$eclSessionDataUrl/session/$internalId"

      when(mockHttpClient.delete(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, "")))

      val result: Unit = await(connector.delete(internalId))
      result shouldBe ()

      verify(mockHttpClient, times(1)).delete(ArgumentMatchers.eq(expectedUrl))(any())

      reset(mockHttpClient)
    }
    "return UpstreamErrorResponse when call to delete session data returns an error " in forAll { internalId: String =>
      val expectedUrl = url"$eclSessionDataUrl/session/$internalId"
      val errorCode   = INTERNAL_SERVER_ERROR

      when(mockHttpClient.delete(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

      Try(await(connector.delete(internalId))) match {
        case Failure(UpstreamErrorResponse(_, code, _, _)) =>
          code shouldEqual errorCode
        case _                                             => fail("expected UpstreamErrorResponse when an error is received from the session data service")
      }
    }
  }

  "upsert" should {
    val expectedUrl = url"$eclSessionDataUrl/session"
    "return unit when request succeeds" in forAll { sessionData: SessionData =>
      when(mockHttpClient.put(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(sessionData)))(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, "")))

      await(connector.upsert(sessionData)) shouldBe ()
    }

    "return a failed future when the http client returns an error response" in forAll { sessionData: SessionData =>
      val errorCode = INTERNAL_SERVER_ERROR

      when(mockHttpClient.put(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(sessionData)))(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

      Try(await(connector.upsert(sessionData))) match {
        case Failure(UpstreamErrorResponse(_, code, _, _)) =>
          code shouldEqual errorCode
        case _                                             => fail("expected UpstreamErrorResponse when an error is received from the session data service")
      }
    }
  }

}
