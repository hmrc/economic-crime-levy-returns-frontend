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

class ContactNameFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "contactName.error.required"
  val lengthKey   = "contactName.error.length"

  val form = new ContactNameFormProvider()()

  "value" should {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringFromRegex(MinMaxValues.NameMaxLength, Regex.NameRegex)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = MinMaxValues.NameMaxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(MinMaxValues.NameMaxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "fail to bind an invalid name" in forAll(
      stringsWithMaxLength(MinMaxValues.NameMaxLength).retryUntil(!_.matches(Regex.NameRegex))
    ) { invalidName: String =>
      val result: Form[String] = form.bind(Map("value" -> invalidName))

      result.errors.map(_.message) should contain only "contactName.error.invalid"
    }
  }
}
