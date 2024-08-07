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

import cats.data.OptionT
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.economiccrimelevyreturns.connectors.ReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, DataValidationError}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn, FirstTimeReturn}
import uk.gov.hmrc.economiccrimelevyreturns.{ValidEclReturn, ValidGetEclReturnSubmissionResponse}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class ReturnsServiceSpec extends ServiceSpec {
  val mockEclReturnsConnector: ReturnsConnector = mock[ReturnsConnector]
  val mockAuditService: AuditService            = mock[AuditService]
  val service                                   = new ReturnsService(
    mockEclReturnsConnector,
    mockAuditService
  )

  override def beforeEach(): Unit = {
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

      result shouldBe Left(DataHandlingError.BadGateway(s"Get Return Submission Failed - $message", errorCode))
    }

    "return DataHandlingError.BAD_REQUEST when when call to returns connector fails with 4xx error" in {
      val errorCode = BAD_REQUEST
      val message   = "BAD_REQUEST"

      when(
        mockEclReturnsConnector
          .getEclReturnSubmission(ArgumentMatchers.eq(periodKey), ArgumentMatchers.eq(eclRegistrationReference))(
            any()
          )
      )
        .thenReturn(Future.failed(UpstreamErrorResponse.apply(message, errorCode)))

      val result =
        await(service.getEclReturnSubmission(periodKey, eclRegistrationReference).value)

      result shouldBe Left(DataHandlingError.BadGateway(s"Get Return Submission Failed - $message", errorCode))
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

  "transformSubmissionToEclReturn" should {
    "return a transformed valid submission to a valid ecl return" in forAll {
      (
        validEclReturn: ValidEclReturn,
        validGetEclReturnSubmissionResponse: ValidGetEclReturnSubmissionResponse,
        calculatedLiability: CalculatedLiability
      ) =>
        val result =
          service
            .transformEclReturnSubmissionToEclReturn(
              validGetEclReturnSubmissionResponse.response,
              Some(validEclReturn.eclReturn),
              calculatedLiability
            )

        result.isRight shouldBe true
    }

    "return DataHandlingError.NotFound when EclReturn is None" in {
      val result = service.transformEclReturnSubmissionToEclReturn(null, None, null)

      result shouldBe Left(DataHandlingError.NotFound("Ecl return not found"))
    }
  }

  "getReturn" should {
    "return normally if success" in forAll { eclReturn: EclReturn =>
      val internalId = eclReturn.internalId

      when(mockEclReturnsConnector.getReturn(ArgumentMatchers.eq(internalId))(any()))
        .thenReturn(Future.successful(eclReturn))

      val result = await(service.getReturn(internalId).value)
      result shouldBe Right(Some(eclReturn))
    }

    "return None if requested return cannot be found" in forAll { internalId: String =>
      val code = NOT_FOUND

      when(mockEclReturnsConnector.getReturn(ArgumentMatchers.eq(internalId))(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(code.toString, code)))

      val result = await(service.getReturn(internalId).value)
      result shouldBe Right(None)
    }

    "return an error if failure" in forAll { (internalId: String, is5xxError: Boolean) =>
      val code = getErrorCode(is5xxError)

      when(mockEclReturnsConnector.getReturn(ArgumentMatchers.eq(internalId))(any()))
        .thenReturn(Future.failed(testException))

      await(service.getReturn(internalId).value) shouldBe
        Left(DataHandlingError.InternalUnexpectedError(Some(testException)))

      if (code != NOT_FOUND) {
        when(mockEclReturnsConnector.getReturn(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse(code.toString, code)))

        await(service.getReturn(internalId).value) shouldBe
          Left(DataHandlingError.BadGateway(s"Get Return Failed - ${code.toString}", code))
      }
    }

    "getReturnValidationErrors" should {
      "return normally if success" in forAll { internalId: String =>
        when(mockEclReturnsConnector.validateEclReturn(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(OptionT[Future, String](Future.successful(None)))

        val result = await(service.getReturnValidationErrors(internalId).value)
        result shouldBe Right(None)
      }

      "return validation error if there is one" in forAll { internalId: String =>
        when(mockEclReturnsConnector.validateEclReturn(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(OptionT[Future, String](Future.successful(Some(internalId))))

        val result = await(service.getReturnValidationErrors(internalId).value)
        result shouldBe Right(Some(DataValidationError(internalId)))
      }

      "return an error if failure" in forAll { (internalId: String, is5xxError: Boolean) =>
        val code = getErrorCode(is5xxError)

        when(mockEclReturnsConnector.validateEclReturn(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(OptionT[Future, String](Future.failed(testException)))

        await(service.getReturnValidationErrors(internalId).value) shouldBe
          Left(DataHandlingError.InternalUnexpectedError(Some(testException)))

        when(mockEclReturnsConnector.validateEclReturn(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(OptionT[Future, String](Future.failed(UpstreamErrorResponse(code.toString, code))))

        await(service.getReturnValidationErrors(internalId).value) shouldBe
          Left(DataHandlingError.BadGateway(s"Get Return Validation Errors Failed - ${code.toString}", code))
      }
    }
  }
}
