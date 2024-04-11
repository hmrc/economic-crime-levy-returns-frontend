/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyreturns.controllers

import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.errors._

import scala.concurrent.Future

class ErrorHandlerSpec extends SpecBase with ErrorHandler {

  def testError(error: DataHandlingError, expected: ResponseError): Unit = {
    val result = await(EitherT.fromEither[Future](Left(error)).asResponseError.value)
    result shouldBe Left(expected)
  }

  def testError(error: EclAccountError, expected: ResponseError): Unit = {
    val result = await(EitherT.fromEither[Future](Left(error)).asResponseError.value)
    result shouldBe Left(expected)
  }

  def testError(error: SessionError, expected: ResponseError): Unit = {
    val result = await(EitherT.fromEither[Future](Left(error)).asResponseError.value)
    result shouldBe Left(expected)
  }

  def testError(error: LiabilityCalculationError, expected: ResponseError): Unit = {
    val result = await(EitherT.fromEither[Future](Left(error)).asResponseError.value)
    result shouldBe Left(expected)
  }

  def testError(error: EmailSubmissionError, expected: ResponseError): Unit = {
    val result = await(EitherT.fromEither[Future](Left(error)).asResponseError.value)
    result shouldBe Left(expected)
  }

  "asResponseError" should {
    "handle errors correctly" in forAll { (message: String, code: Int) =>
      testError(
        DataHandlingError.BadGateway(message, code),
        ResponseError.badGateway(message, code)
      )
      testError(
        DataHandlingError.InternalUnexpectedError(None),
        ResponseError.internalServiceError(cause = None)
      )
      testError(
        DataHandlingError.NotFound(message),
        ResponseError.internalServiceError(message)
      )

      testError(
        EclAccountError.BadGateway(message, code),
        ResponseError.badGateway(message, code)
      )
      testError(
        EclAccountError.InternalUnexpectedError(None),
        ResponseError.internalServiceError(cause = None)
      )

      testError(
        SessionError.BadGateway(message, code),
        ResponseError.badGateway(message, code)
      )
      testError(
        SessionError.InternalUnexpectedError(message, None),
        ResponseError.internalServiceError(message = message, cause = None)
      )
      testError(
        SessionError.NotFound(),
        ResponseError.notFoundError("Session not found")
      )
      testError(
        SessionError.KeyNotFound(message),
        ResponseError.internalServiceError(message = s"Key not found in session: $message", cause = None)
      )

      testError(
        EmailSubmissionError.BadGateway(message, code),
        ResponseError.badGateway(message, code)
      )
      testError(
        EmailSubmissionError.InternalUnexpectedError(None),
        ResponseError.internalServiceError(cause = None)
      )

      testError(
        LiabilityCalculationError.BadGateway(message, code),
        ResponseError.badGateway(message, code)
      )
      testError(
        LiabilityCalculationError.InternalUnexpectedError(None),
        ResponseError.internalServiceError(cause = None)
      )
      testError(
        LiabilityCalculationError.BadRequest(message),
        ResponseError.badRequestError(message)
      )
    }
  }

  "dataHandlingErrorConverter" should {
    "return ResponseError.internalServiceError when nothing is converted" in {
      val result: ResponseError = dataHandlingErrorConverter.convert(null)

      result shouldBe ResponseError.internalServiceError(
        "Invalid DataHandlingError",
        ErrorCode.InternalServerError,
        None
      )
    }
  }

  "eclAccountErrorConverter" should {
    "return ResponseError.internalServiceError when nothing is converted" in {
      val result: ResponseError = eclAccountErrorConverter.convert(null)

      result shouldBe ResponseError.internalServiceError(
        "Invalid EclAccountError",
        ErrorCode.InternalServerError,
        None
      )
    }
  }

  "eclEmailSubmissionError" should {
    "return ResponseError.internalServiceError when nothing is converted" in {
      val result: ResponseError = eclEmailSubmissionError.convert(null)

      result shouldBe ResponseError.internalServiceError(
        "Invalid EmailSubmissionError",
        ErrorCode.InternalServerError,
        None
      )
    }
  }

  "liabilityCalculationErrorConverter" should {
    "return ResponseError.internalServiceError when nothing is converted" in {
      val result: ResponseError = liabilityCalculationErrorConverter.convert(null)

      result shouldBe ResponseError.internalServiceError(
        "Invalid LiabilityCalculationError",
        ErrorCode.InternalServerError,
        None
      )
    }
  }

  "sessionErrorConverter" should {
    "return ResponseError.internalServiceError when nothing is converted" in {
      val result: ResponseError = sessionErrorConverter.convert(null)

      result shouldBe ResponseError.internalServiceError(
        "Invalid SessionError",
        ErrorCode.InternalServerError,
        None
      )
    }
  }
}
