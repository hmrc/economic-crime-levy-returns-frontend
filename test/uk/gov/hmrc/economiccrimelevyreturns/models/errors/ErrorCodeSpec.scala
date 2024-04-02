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

import org.scalacheck.Arbitrary
import play.api.libs.json.{JsError, JsString, Json}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ErrorCode.errorCodes

class ErrorCodeSpec extends SpecBase {

  "reads/writes" should {
    "behave as expected" in forAll { error: ErrorCode =>
      val json = Json.toJson(error)
      Json.fromJson[ErrorCode](json).asOpt shouldBe Some(error)
    }

    "return an error if invalid" in forAll(
      Arbitrary.arbitrary[String].retryUntil(!errorCodes.map(_.code).contains(_))
    ) { text: String =>
      Json.fromJson[ErrorCode](JsString(text)) shouldBe JsError()
    }
  }
}
