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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, NO_CONTENT}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionData
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException, HttpClient, HttpResponse, InternalServerException, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SessionDataConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new SessionDataConnector(appConfig, mockHttpClient)
  val eclSessionDataUrl          = "http://localhost:14003/economic-crime-levy-returns"

  "get" should {

    "return SessionData data when request succeeds" in forAll { (internalId: String, sessionData: SessionData) =>
      val expectedUrl = s"$eclSessionDataUrl/session/$internalId"

      when(
        mockHttpClient.GET[SessionData](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any())
      )
        .thenReturn(Future.successful(sessionData))

      val result = await(connector.get(internalId))

      result shouldBe sessionData

      verify(mockHttpClient, times(1))
        .GET[SessionData](
          ArgumentMatchers.eq(expectedUrl),
          any(),
          any()
        )(any(), any(), any())

      reset(mockHttpClient)
    }

    "return none when the http client returns none" in forAll { internalId: String =>
      val expectedUrl = s"$eclSessionDataUrl/session/$internalId"
      val msg         = "not found"
      when(
        mockHttpClient.GET[SessionData](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any())
      )
        .thenReturn(Future.failed(new BadGatewayException(msg)))

      Try(await(connector.get(internalId))) match {
        case Failure(thr) => thr.getMessage shouldBe msg
        case Success(_)   => fail("expected exception to be thrown")
      }

      verify(mockHttpClient, times(1))
        .GET[SessionData](
          ArgumentMatchers.eq(expectedUrl),
          any(),
          any()
        )(any(), any(), any())

      reset(mockHttpClient)
    }
  }

  "delete" should {
    "return unit when the http client successfully returns a http response" in forAll { internalId: String =>
      val expectedUrl = s"$eclSessionDataUrl/session/$internalId"

      when(
        mockHttpClient.DELETE[Unit](ArgumentMatchers.eq(expectedUrl), any())(
          any(),
          any(),
          any()
        )
      )
        .thenReturn(Future.successful(()))

      val result: Unit = await(connector.delete(internalId))
      result shouldBe ()

      verify(mockHttpClient, times(1))
        .DELETE[Either[UpstreamErrorResponse, HttpResponse]](
          ArgumentMatchers.eq(expectedUrl),
          any()
        )(any(), any(), any())

      reset(mockHttpClient)
    }

    "return a failed future when the http client returns an error response" in forAll { internalId: String =>
      val expectedUrl = s"$eclSessionDataUrl/session/$internalId"
      val msg         = "timeout error"

      when(
        mockHttpClient.DELETE[Unit](ArgumentMatchers.eq(expectedUrl), any())(
          any(),
          any(),
          any()
        )
      )
        .thenReturn(Future.failed(new GatewayTimeoutException(msg)))

      Try(await(connector.delete(internalId))) match {
        case Failure(thr) => thr.getMessage shouldBe msg
        case Success(_)   => fail("expected exception to be thrown")
      }

      verify(mockHttpClient, times(1))
        .DELETE[Unit](
          ArgumentMatchers.eq(expectedUrl),
          any()
        )(any(), any(), any())

      reset(mockHttpClient)
    }
  }

  "upsert" should {
    val expectedUrl = s"$eclSessionDataUrl/session"
    "return unit when request succeds" in forAll { sessionData: SessionData =>
      when(
        mockHttpClient
          .PUT[SessionData, Unit](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any(), any())
      )
        .thenReturn(Future.successful(()))

      val result = await(connector.upsert(sessionData))
      result shouldBe ()

      verify(mockHttpClient, times(1))
        .PUT[SessionData, Unit](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any(), any())

      reset(mockHttpClient)
    }

    "return a failed future when the http client returns an error response" in forAll { sessionData: SessionData =>
      val expectedUrl = s"$eclSessionDataUrl/session"
      val msg         = "internal server error"

      when(
        mockHttpClient
          .PUT[SessionData, Unit](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any(), any())
      )
        .thenReturn(Future.failed(new InternalServerException(msg)))

      Try(await(connector.upsert(sessionData))) match {
        case Failure(thr) => thr.getMessage shouldBe msg
        case Success(_)   => fail("expected exception to be thrown")
      }

      verify(mockHttpClient, times(1))
        .PUT[SessionData, Unit](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any(), any())

      reset(mockHttpClient)
    }
  }

}
