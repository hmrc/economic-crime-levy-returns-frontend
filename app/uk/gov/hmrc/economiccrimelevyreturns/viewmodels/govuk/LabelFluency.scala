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

package uk.gov.hmrc.economiccrimelevyreturns.viewmodels.govuk

import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.LabelSize
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Content
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label

object label extends LabelFluency

trait LabelFluency {

  object LabelViewModel {

    def apply(content: Content): Label =
      Label(content = content)
  }

  implicit class FluentLabel(label: Label) {

    def withCssClass(className: String): Label =
      label.copy(classes = s"${label.classes} $className")

    def asHidden(): Label =
      label
        .copy(isPageHeading = false)
        .withCssClass("govuk-visually-hidden")

    def withSize(size: LabelSize = LabelSize.Medium): Label =
      label
        .withCssClass(size.toString)
  }
}
