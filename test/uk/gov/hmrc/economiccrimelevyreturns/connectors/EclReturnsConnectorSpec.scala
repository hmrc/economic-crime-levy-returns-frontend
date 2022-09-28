/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.http.{HttpClient, HttpResponse}

import scala.concurrent.Future

class EclReturnsConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new EclReturnsConnector(appConfig, mockHttpClient)

  "getReturn" should {
    "return an ecl return when the Http Client returns an ecl return" in {
      when(mockHttpClient.GET[Option[EclReturn]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(emptyReturn)))

      val result = await(connector.getReturn(internalId))
      result shouldBe Some(emptyReturn)
    }

    "return none when the http client returns none" in {
      when(mockHttpClient.GET[Option[EclReturn]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = await(connector.getReturn(internalId))
      result shouldBe None
    }
  }

  "deleteReturn" should {
    "return unit when the http client successfully returns a http response" in {
      val response = HttpResponse(NO_CONTENT, "", Map.empty)

      when(mockHttpClient.DELETE[HttpResponse](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(response))

      val result = await(connector.deleteReturn(internalId))
      result shouldBe ()
    }
  }

  "upsertReturn" should {
    "return the new or updated ecl return" in {
      when(mockHttpClient.PUT[EclReturn, EclReturn](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(emptyReturn))

      val result = await(connector.upsertReturn(emptyReturn))
      result shouldBe emptyReturn
    }
  }
}
