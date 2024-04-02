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

import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Content
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._

object summarylist extends SummaryListFluency

trait SummaryListFluency {

  object SummaryListViewModel {

    def apply(rows: Seq[SummaryListRow]): SummaryList =
      SummaryList(rows = rows)
  }

  implicit class FluentSummaryList(list: SummaryList) {

    def withCssClass(className: String): SummaryList =
      list.copy(classes = s"${list.classes} $className")
  }

  object SummaryListRowViewModel {

    def apply(
      key: Key,
      value: Value
    ): SummaryListRow =
      SummaryListRow(
        key = key,
        value = value
      )

    def apply(
      key: Key,
      value: Value,
      actions: Seq[ActionItem]
    ): SummaryListRow =
      SummaryListRow(
        key = key,
        value = value,
        actions = Some(Actions(items = actions))
      )
  }

  object ActionItemViewModel {

    def apply(
      content: Content,
      href: String
    ): ActionItem =
      ActionItem(
        content = content,
        href = href
      )
  }

  implicit class FluentActionItem(actionItem: ActionItem) {

    def withVisuallyHiddenText(text: String): ActionItem =
      actionItem.copy(visuallyHiddenText = Some(text))
  }

  object ValueViewModel {

    def apply(content: Content): Value =
      Value(content = content)
  }
}
