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

package uk.gov.hmrc.economiccrimelevyreturns.services

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.PartialFunctionValues
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataHandlingError
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class BaseServiceSpec extends SpecBase with PartialFunctionValues {

  class TestService extends BaseService

  val service: TestService = new TestService

  val testException = new Exception("error")

  "handleError" should {
    "behave as expected" in forAll(
      Arbitrary.arbitrary[String],
      Gen.chooseNum[Int](401, 599)
    ) { (message: String, code: Int) =>
      val result = service.handleError
      result.valueAt(testException)                        shouldBe Left(DataHandlingError.InternalUnexpectedError(Some(testException)))
      result.valueAt(UpstreamErrorResponse(message, code)) shouldBe Left(
        DataHandlingError.BadGateway(reason = message, code = code)
      )
    }
  }
}
