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
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.KeyValue
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.{EclEnrolment, QueryKnownFactsRequest, QueryKnownFactsResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import scala.util.{Failure, Try}
import scala.concurrent.Future

class EnrolmentStoreProxyConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new EnrolmentStoreProxyConnectorImpl(appConfig, mockHttpClient, config, actorSystem)
  val enrolmentStoreUrl: String          = s"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store-proxy/enrolment-store"

  override def beforeEach() = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  "queryKnownFacts" should {
    "return known facts when the http client returns known facts" in forAll {
      (eclRegistrationReference: String, queryKnownFactsResponse: QueryKnownFactsResponse) =>
        beforeEach()

        val expectedUrl                    = url"$enrolmentStoreUrl/enrolments"
        val expectedQueryKnownFactsRequest = QueryKnownFactsRequest(
          service = EclEnrolment.ServiceName,
          knownFacts = Seq(
            KeyValue(EclEnrolment.IdentifierKey, eclRegistrationReference)
          )
        )

        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(
          mockRequestBuilder
            .withBody(ArgumentMatchers.eq(Json.toJson(expectedQueryKnownFactsRequest)))(any(), any(), any())
        )
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(queryKnownFactsResponse)))))

        val result = await(connector.queryKnownFacts(eclRegistrationReference))

        result shouldBe queryKnownFactsResponse
    }

    "return 5xx UpstreamErrorResponse when call to calculator service returns an error and executes retry" in {
      (eclRegistrationReference: String) =>
        val errorCode                      = INTERNAL_SERVER_ERROR
        val expectedUrl                    = url"$enrolmentStoreUrl/enrolments"
        val expectedQueryKnownFactsRequest = QueryKnownFactsRequest(
          service = EclEnrolment.ServiceName,
          knownFacts = Seq(
            KeyValue(EclEnrolment.IdentifierKey, eclRegistrationReference)
          )
        )

        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(ArgumentMatchers.eq(expectedQueryKnownFactsRequest))(any(), any(), any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

        Try(await(connector.queryKnownFacts(any()))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual errorCode
          case _                                             => fail("expected UpstreamErrorResponse when an error is received from the account service")
        }

        verify(mockRequestBuilder, times(4)).execute(any(), any())
    }
  }
}
