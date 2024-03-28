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

final case class ReturnSubmittedEmailParameters(
  name: String,
  dateSubmitted: String,
  periodStartDate: String,
  periodEndDate: String,
  chargeReference: Option[String],
  fyStartYear: String,
  fyEndYear: String,
  datePaymentDue: Option[String],
  amountDue: String
)

object ReturnSubmittedEmailParameters {
  implicit val format: OFormat[ReturnSubmittedEmailParameters] = Json.format[ReturnSubmittedEmailParameters]
}

final case class ReturnSubmittedEmailRequest(
  to: Seq[String],
  templateId: String,
  parameters: ReturnSubmittedEmailParameters,
  force: Boolean = false,
  eventUrl: Option[String] = None
)

object ReturnSubmittedEmailRequest {
  val nilReturnTemplateId: String                           = "ecl_nil_return_submitted"
  val returnTemplateId: String                              = "ecl_return_submitted"
  implicit val format: OFormat[ReturnSubmittedEmailRequest] = Json.format[ReturnSubmittedEmailRequest]
}
