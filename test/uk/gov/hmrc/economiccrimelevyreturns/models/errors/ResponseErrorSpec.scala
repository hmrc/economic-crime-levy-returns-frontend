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

package uk.gov.hmrc.economiccrimelevyreturns.models.errors

import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.http.UpstreamErrorResponse

class ResponseErrorSpec extends SpecBase {

  "error methods" should {
    "behave as expected" in forAll(
      Arbitrary.arbitrary[String],
      Gen.chooseNum[Int](401, 599)
    ) { (message: String, code: Int) =>
      ResponseError.badRequestError(message) shouldBe
        StandardError(message, ErrorCode.BadRequest)

      ResponseError.notFoundError(message) shouldBe
        StandardError(message, ErrorCode.NotFound)

      ResponseError.unauthorized(message) shouldBe
        StandardError(message, ErrorCode.Unauthorized)

      ResponseError.badGateway(message, code) shouldBe
        uk.gov.hmrc.economiccrimelevyreturns.models.errors.BadGateway(message, ErrorCode.BadGateway, code)

      ResponseError.upstreamServiceError(
        message,
        ErrorCode.forCode(code),
        UpstreamErrorResponse(message, code)
      ) shouldBe UpstreamServiceError(
        message,
        ErrorCode.forCode(code),
        UpstreamErrorResponse(message, code)
      )

      ResponseError.internalServiceError(message, ErrorCode.forCode(code)) shouldBe InternalServiceError(
        message,
        ErrorCode.forCode(code),
        None
      )
    }
  }
}
