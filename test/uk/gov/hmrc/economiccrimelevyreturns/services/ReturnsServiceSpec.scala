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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.economiccrimelevyreturns.ValidGetEclReturnSubmissionResponse
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.ReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataHandlingError
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

  override def beforeEach() = {
    reset(mockAuditService)
    reset(mockEclReturnsConnector)
  }

  "getOrCreateReturn" should {
    "return a created ecl return when one does not exist" in forAll {
      (internalId: String, eclReturn: EclReturn, eclReference: String) =>
        beforeEach()

        val newReturn = EclReturn.empty(internalId, Some(eclReturn.returnType.getOrElse(FirstTimeReturn)))
        when(mockEclReturnsConnector.getReturn(any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse.apply("Not found", NOT_FOUND)))

        when(mockEclReturnsConnector.upsertReturn(ArgumentMatchers.eq(newReturn))(any()))
          .thenReturn(Future.successful(()))

        val result = await(
          service
            .getOrCreateReturn(internalId, eclReturn.returnType)(
              hc,
              AuthorisedRequest(fakeRequest, internalId, eclReference)
            )
            .value
        )

        result shouldBe Right(newReturn)

        verify(mockAuditService, times(1)).auditReturnStarted(anyString(), anyString(), any())(any())

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

  "getEclReturnSubmission" should {
    "return a return submission response when call to returns connector succeeds" in forAll {
      (
        validResponse: ValidGetEclReturnSubmissionResponse
      ) =>
        when(
          mockEclReturnsConnector
            .getEclReturnSubmission(ArgumentMatchers.eq(periodKey), ArgumentMatchers.eq(eclRegistrationReference))(
              any()
            )
        )
          .thenReturn(Future.successful(validResponse.response))

        val result = await(service.getEclReturnSubmission(periodKey, eclRegistrationReference).value)

        result shouldBe Right(validResponse.response)
    }

    "return DataHandlingError.BadGateway when when call to returns connector fails with 5xx error" in {

      val errorCode = INTERNAL_SERVER_ERROR
      val message   = "INTERNAL_SERVER_ERROR"

      when(
        mockEclReturnsConnector
          .getEclReturnSubmission(ArgumentMatchers.eq(periodKey), ArgumentMatchers.eq(eclRegistrationReference))(
            any()
          )
      )
        .thenReturn(Future.failed(UpstreamErrorResponse.apply(message, errorCode)))

      val result =
        await(service.getEclReturnSubmission(periodKey, eclRegistrationReference).value)

      result shouldBe Left(DataHandlingError.BadGateway(message, errorCode))
    }

    "return DataHandlingError.InternalUnexpectedError when when call to returns connector fails with an unexpected error" in {

      val throwable: Exception = new Exception()

      when(
        mockEclReturnsConnector
          .getEclReturnSubmission(ArgumentMatchers.eq(periodKey), ArgumentMatchers.eq(eclRegistrationReference))(
            any()
          )
      )
        .thenReturn(Future.failed(throwable))

      val result =
        await(service.getEclReturnSubmission(periodKey, eclRegistrationReference).value)

      result shouldBe Left(DataHandlingError.InternalUnexpectedError(Some(throwable), None))
    }
  }
}
