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

package uk.gov.hmrc.economiccrimelevyreturns.models

import play.api.libs.json.{JsError, JsNull, JsString, Json}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase

class ObligationDataSpec extends SpecBase {

  "read" should {
    "return None if passed None" in {
      ObligationDetails.read(None) shouldBe None
    }

    "return an error if incorrect Json" in {
      val s       = "A"
      val result1 = Json.fromJson[ObligationStatus](JsString(s))
      result1 shouldBe JsError(s"$s is not a valid ObligationStatus")

      val result2 = Json.fromJson[ObligationStatus](JsNull)
      result2.isError shouldBe true
    }
  }
}
