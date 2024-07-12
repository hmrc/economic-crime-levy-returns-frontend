/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase

class CalculateLiabilityRequestModelSpec extends SpecBase {

  val json: JsObject = Json.obj(
    "amlRegulatedActivityLength" -> 1,
    "relevantApLength"           -> 2,
    "ukRevenue"                  -> 200,
    "year"                       -> 2000
  )

  val model: CalculateLiabilityRequest = CalculateLiabilityRequest(1, 2, 200, 2000)

  "CalculateLiabilityRequestModel" should {

    "read from JSON" in {
      json.as[CalculateLiabilityRequest] shouldBe model
    }

    "write to JSON" in {
      Json.toJson(model) shouldBe json
    }
  }
}
