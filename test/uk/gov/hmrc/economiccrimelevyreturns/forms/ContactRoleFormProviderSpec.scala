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

class ContactRoleFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "contactRole.error.required"
  val lengthKey   = "contactRole.error.length"

  val form = new ContactRoleFormProvider()()

  "value" should {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringFromRegex(MinMaxValues.roleMaxLength, Regex.positionInCompanyRegex)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = MinMaxValues.roleMaxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(MinMaxValues.roleMaxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "fail to bind an invalid role" in forAll(
      stringsWithMaxLength(MinMaxValues.roleMaxLength).retryUntil(!_.matches(Regex.positionInCompanyRegex))
    ) { invalidRole: String =>
      val result: Form[String] = form.bind(Map("value" -> invalidRole))

      result.errors.map(_.message) should contain only "contactRole.error.invalid"
    }
  }
}
