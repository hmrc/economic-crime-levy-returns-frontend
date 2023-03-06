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

package uk.gov.hmrc.economiccrimelevyreturns.forms

import play.api.data.Form
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.Mappings

import javax.inject.Inject

class AmlRegulatedActivityLengthFormProvider @Inject() extends Mappings {

  val minDays = 1
  val maxDays = 999

  def apply(): Form[Int] =
    Form(
      "value" -> int(
        "amlRegulatedActivityLength.error.required",
        "amlRegulatedActivityLength.error.wholeNumber",
        "amlRegulatedActivityLength.error.nonNumeric"
      )
        .verifying(inRange(minDays, maxDays, "amlRegulatedActivityLength.error.outOfRange"))
    )

}