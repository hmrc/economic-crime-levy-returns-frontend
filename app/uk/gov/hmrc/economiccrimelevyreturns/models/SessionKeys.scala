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

package uk.gov.hmrc.economiccrimelevyreturns.models

import play.api.mvc.Session

object SessionKeys {

  val amountDue: String         = "amountDue"
  val band: String              = "band"
  val chargeReference: String   = "chargeReference"
  val email: String             = "email"
  val isIncrease: String        = "isIncrease"
  val obligationDetails: String = "obligationDetails"
  val periodKey: String         = "periodKey"
  val returnType: String        = "returnType"
  val startAmendUrl: String     = "StartAmendUrl"
  val urlToReturnTo: String     = "UrlToReturnTo"

  implicit class SessionOps(s: Session) {
    def clearEclValues: Session = s -- Seq(chargeReference, email, amountDue, obligationDetails, startAmendUrl)
  }
}
