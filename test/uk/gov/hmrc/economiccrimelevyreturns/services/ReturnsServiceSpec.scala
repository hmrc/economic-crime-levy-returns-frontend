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

package uk.gov.hmrc.economiccrimelevyreturns.services

import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.ReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, FirstTimeReturn}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class ReturnsServiceSpec extends SpecBase {
  val mockEclReturnsConnector: ReturnsConnector = mock[ReturnsConnector]
  val mockAuditService: AuditService            = mock[AuditService]
  val service                                   = new ReturnsService(
    mockEclReturnsConnector,
    mockAuditService
  )

  "getOrCreateReturn" should {
    "return a created ecl return when one does not exist" in forAll {
      (internalId: String, eclReturn: EclReturn, eclReference: String) =>
        when(mockEclReturnsConnector.getReturn(any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse.apply("Not found", NOT_FOUND)))

        val newReturn = EclReturn.empty(internalId, Some(eclReturn.returnType.getOrElse(FirstTimeReturn)))

        when(mockEclReturnsConnector.upsertReturn(any())(any()))
          .thenReturn(Future.successful(()))

        val result = await(
          service
            .getOrCreateReturn(internalId)(hc, AuthorisedRequest(fakeRequest, internalId, eclReference))
            .value
        )
        result shouldBe Right(newReturn)

        verify(mockAuditService, times(1)).auditReturnStarted(anyString(), anyString(), any())

        reset(mockAuditService)
    }

    "return an existing ecl return" in forAll { (internalId: String, eclReturn: EclReturn, eclReference: String) =>
      when(mockEclReturnsConnector.getReturn(any())(any()))
        .thenReturn(Future.successful(eclReturn))

      val result =
        await(service.getOrCreateReturn(internalId)(hc, AuthorisedRequest(fakeRequest, internalId, eclReference)).value)

      result shouldBe Right(eclReturn)
    }
  }
}
