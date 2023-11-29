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
import uk.gov.hmrc.economiccrimelevyreturns.forms.behaviours.CurrencyFieldBehaviours
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues

class UkRevenueFormProviderSpec extends CurrencyFieldBehaviours {

  val form = new UkRevenueFormProvider()()

  "value" should {

    val fieldName          = "value"
    val RevenueMin: Double = 0.0
    val RevenueMax: Double = 99999999999.99

    val validDataGenerator = bigDecimalInRangeWithCommas(RevenueMin, RevenueMax)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like currencyField(
      form,
      fieldName,
      nonNumericError = FormError(fieldName, "ukRevenue.error.nonNumeric")
    )

    behave like currencyFieldWithRange(
      form,
      fieldName,
      minimum = RevenueMin,
      maximum = RevenueMax,
      expectedError =
        FormError(fieldName, "ukRevenue.error.outOfRange", Seq(MinMaxValues.RevenueMin, MinMaxValues.RevenueMax))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "ukRevenue.error.required")
    )
  }
}
