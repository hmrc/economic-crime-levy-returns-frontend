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
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.email.AmendReturnSubmittedRequest.AmendReturnTemplateId
import uk.gov.hmrc.economiccrimelevyreturns.models.email.ReturnSubmittedEmailRequest.{NilReturnTemplateId, ReturnTemplateId}
import uk.gov.hmrc.economiccrimelevyreturns.models.email.{AmendReturnSubmittedParameters, AmendReturnSubmittedRequest, ReturnSubmittedEmailParameters, ReturnSubmittedEmailRequest}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class EmailConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new EmailConnector(appConfig, mockHttpClient, config, actorSystem)
  private val expectedUrl                = url"${appConfig.emailBaseUrl}/hmrc/email"

  override def beforeEach() = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  "sendReturnSubmittedEmail" should {
    "return unit when the http client returns ACCEPTED for the return submitted email template" in forAll {
      (to: String, returnSubmittedEmailParameters: ReturnSubmittedEmailParameters) =>
        val templateId =
          if (returnSubmittedEmailParameters.chargeReference.isDefined) ReturnTemplateId else NilReturnTemplateId

        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(
          mockRequestBuilder.withBody(
            ArgumentMatchers.eq(
              Json.toJson(
                ReturnSubmittedEmailRequest(
                  Seq(to),
                  templateId = templateId,
                  returnSubmittedEmailParameters
                )
              )
            )
          )(any(), any(), any())
        ).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(ACCEPTED, "")))

        val result: Unit = await(connector.sendReturnSubmittedEmail(to, returnSubmittedEmailParameters))

        result shouldBe ()
    }

    "return 5xx UpstreamErrorResponse when call to account service returns an error and executes retry" in forAll {
      (to: String, returnSubmittedEmailParameters: ReturnSubmittedEmailParameters) =>
        beforeEach()

        val errorCode = INTERNAL_SERVER_ERROR

        val templateId =
          if (returnSubmittedEmailParameters.chargeReference.isDefined) ReturnTemplateId else NilReturnTemplateId

        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(
          mockRequestBuilder.withBody(
            ArgumentMatchers.eq(
              Json.toJson(
                ReturnSubmittedEmailRequest(
                  Seq(to),
                  templateId = templateId,
                  returnSubmittedEmailParameters,
                  force = false,
                  None
                )
              )
            )
          )(any(), any(), any())
        ).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

        Try(await(connector.sendReturnSubmittedEmail(to, returnSubmittedEmailParameters))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual errorCode
          case _                                             => fail("expected UpstreamErrorResponse when an error is received from the account service")
        }

        verify(mockRequestBuilder, times(4))
          .execute(any(), any())
    }
  }

  "sendAmendReturnSubmittedEmail" should {
    "return unit when the http client returns ACCEPTED for the return submitted email template" in forAll {
      (to: String, amendSubmittedEmailParameters: AmendReturnSubmittedParameters) =>
        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(
          mockRequestBuilder.withBody(
            ArgumentMatchers.eq(
              Json.toJson(
                AmendReturnSubmittedRequest(
                  Seq(to),
                  templateId = AmendReturnTemplateId,
                  amendSubmittedEmailParameters
                )
              )
            )
          )(any(), any(), any())
        ).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(ACCEPTED, "")))

        val result: Unit = await(connector.sendAmendReturnSubmittedEmail(to, amendSubmittedEmailParameters))

        result shouldBe ()
    }

    "return 5xx UpstreamErrorResponse when call to account service returns an error and executes retry" in forAll {
      (to: String, amendSubmittedEmailParameters: AmendReturnSubmittedParameters) =>
        beforeEach()

        val errorCode = INTERNAL_SERVER_ERROR

        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(
          mockRequestBuilder.withBody(
            ArgumentMatchers.eq(
              Json.toJson(
                AmendReturnSubmittedRequest(
                  Seq(to),
                  templateId = AmendReturnTemplateId,
                  amendSubmittedEmailParameters
                )
              )
            )
          )(any(), any(), any())
        ).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

        Try(await(connector.sendAmendReturnSubmittedEmail(to, amendSubmittedEmailParameters))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual errorCode
          case _                                             => fail("expected UpstreamErrorResponse when an error is received from the account service")
        }

        verify(mockRequestBuilder, times(4))
          .execute(any(), any())
    }
  }
}
