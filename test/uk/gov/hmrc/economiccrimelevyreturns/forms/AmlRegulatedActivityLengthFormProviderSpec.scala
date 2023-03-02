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

import play.api.data.FormError
import uk.gov.hmrc.economiccrimelevyreturns.forms.behaviours.IntFieldBehaviours

class AmlRegulatedActivityLengthFormProviderSpec extends IntFieldBehaviours {

  val form = new AmlRegulatedActivityLengthFormProvider()()

  "value" should {

    val fieldName = "value"

    val minimum = 1
    val maximum = 999

    val validDataGenerator = longsInRangeWithCommas(minimum, maximum)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like intField(
      form,
      fieldName,
      nonNumericError = FormError(fieldName, "amlRegulatedActivityLength.error.nonNumeric"),
      wholeNumberError = FormError(fieldName, "amlRegulatedActivityLength.error.wholeNumber")
    )

    behave like intFieldWithRange(
      form,
      fieldName,
      minimum = minimum,
      maximum = maximum,
      expectedError = FormError(fieldName, "amlRegulatedActivityLength.error.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "amlRegulatedActivityLength.error.required")
    )
  }
}
