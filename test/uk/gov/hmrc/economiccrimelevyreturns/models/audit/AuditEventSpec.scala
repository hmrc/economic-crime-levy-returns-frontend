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

package uk.gov.hmrc.economiccrimelevyreturns.models.audit

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase

class AuditEventSpec extends SpecBase {

  case class Data(text: String)

  object Data {
    implicit val format: OFormat[Data] = Json.format[Data]
  }

  val testAuditType: String   = random[String]
  val testDetailJson: JsValue = Json.toJson(Data(testAuditType))

  "extendedDataEvent" should {
    "behave as expected" in {
      val event = new AuditEvent {
        override val auditType: String   = testAuditType
        override val detailJson: JsValue = testDetailJson
      }

      val extended = event.extendedDataEvent

      extended.auditType shouldBe testAuditType
      extended.detail    shouldBe testDetailJson
    }
  }
}
