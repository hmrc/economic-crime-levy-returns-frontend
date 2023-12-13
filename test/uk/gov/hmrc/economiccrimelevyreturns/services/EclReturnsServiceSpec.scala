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

import org.mockito.ArgumentMatchers.any
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.ReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, EclReturn}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.Future

class EclReturnsServiceSpec extends SpecBase {
  val mockEclReturnsConnector: ReturnsConnector = mock[ReturnsConnector]
  val mockAuditConnector: AuditConnector           = mock[AuditConnector]
  val service                                      = new EclReturnsService(
    mockEclReturnsConnector,
    mockAuditConnector
  )

  "getOrCreateReturn" should {
    "return a created ecl return when one does not exist" in forAll {
      (internalId: String, eclReturn: EclReturn, eclReference: String) =>
        when(mockEclReturnsConnector.getReturn(any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse.apply("Not found", NOT_FOUND)))

        when(mockEclReturnsConnector.upsertReturn(any())(any()))
          .thenReturn(Future.successful(eclReturn))

        val result = await(
          service
            .getOrCreateReturn(internalId)(hc, AuthorisedRequest(fakeRequest, internalId, eclReference))
        )
        result shouldBe eclReturn

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }

    "return an existing ecl return" in forAll { (internalId: String, eclReturn: EclReturn, eclReference: String) =>
      when(mockEclReturnsConnector.getReturn(any())(any()))
        .thenReturn(Future.successful(eclReturn))

      val result =
        await(service.getOrCreateReturn(internalId)(hc, AuthorisedRequest(fakeRequest, internalId, eclReference)))
      result shouldBe eclReturn
    }

    "return an updated ecl return and verify number of calls" in forAll {
      (internalId: String, eclReturn: EclReturn, eclReference: String) =>
        reset(mockEclReturnsConnector)

        when(mockEclReturnsConnector.getReturn(any())(any()))
          .thenReturn(Future.successful(eclReturn))

        val updatedEclReturn = eclReturn.copy(returnType = Some(AmendReturn))

        when(mockEclReturnsConnector.upsertReturn(any())(any()))
          .thenReturn(Future.successful(updatedEclReturn))

        val result =
          await(service.upsertEclReturnType(internalId, AmendReturn)(hc))
        result shouldBe updatedEclReturn

        verify(mockEclReturnsConnector, times(1))
          .getReturn(any())(any())

        verify(mockEclReturnsConnector, times(1))
          .upsertReturn(any())(any())
    }
  }
}
