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

package uk.gov.hmrc.economiccrimelevyreturns.models.email

import play.api.libs.json.{Json, OFormat}

final case class AmendReturnSubmittedParameters(
  name: String,
  dateSubmitted: String,
  periodStartDate: String,
  periodEndDate: String
)

object AmendReturnSubmittedParameters {
  implicit val format: OFormat[AmendReturnSubmittedParameters] = Json.format[AmendReturnSubmittedParameters]
}

final case class AmendReturnSubmittedRequest(
  to: Seq[String],
  templateId: String,
  parameters: AmendReturnSubmittedParameters,
  force: Boolean = false,
  eventUrl: Option[String] = None
)

object AmendReturnSubmittedRequest {
  implicit val format: OFormat[AmendReturnSubmittedRequest] = Json.format[AmendReturnSubmittedRequest]
  val AmendReturnTemplateId: String                         = "ecl_amend_return_submitted"
}
