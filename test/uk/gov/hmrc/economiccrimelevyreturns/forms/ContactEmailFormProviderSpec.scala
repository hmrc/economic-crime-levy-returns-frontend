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

import play.api.data.{Form, FormError}
import uk.gov.hmrc.economiccrimelevyreturns.forms.behaviours.StringFieldBehaviours
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.{MinMaxValues, Regex}

class ContactEmailFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "contactEmail.error.required"
  val lengthKey   = "contactEmail.error.length"

  val form = new ContactEmailFormProvider()()

  "value" should {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      emailAddress(MinMaxValues.emailMaxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = MinMaxValues.emailMaxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(MinMaxValues.emailMaxLength)),
      emailAddressMoreThanMaxLength(MinMaxValues.emailMaxLength)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "fail to bind an invalid email address" in forAll(
      stringsWithMaxLength(MinMaxValues.emailMaxLength).retryUntil(!_.matches(Regex.emailRegex))
    ) { invalidEmail: String =>
      val result: Form[String] = form.bind(Map("value" -> invalidEmail))

      result.errors.map(_.message) should contain("contactEmail.error.invalid")
    }
  }
}
