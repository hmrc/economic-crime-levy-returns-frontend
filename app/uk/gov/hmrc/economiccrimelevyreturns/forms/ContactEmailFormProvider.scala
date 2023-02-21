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

class ContactEmailFormProvider @Inject() extends Mappings {

  private val maxLength = 160

  def apply(): Form[String] =
    Form(
      "value" -> text("contactEmail.error.required")
        .transform[String](_.toLowerCase, identity)
        .verifying(emailAddress(maxLength, "contactEmail.error.length", "contactEmail.error.invalid"))
    )
}
