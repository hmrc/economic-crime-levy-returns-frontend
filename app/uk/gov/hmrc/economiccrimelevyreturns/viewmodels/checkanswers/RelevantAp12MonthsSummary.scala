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

package uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers

import play.api.i18n.Messages
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.CheckMode
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.implicits._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}

object RelevantAp12MonthsSummary {

  def row(relevantAp12Months: Option[Boolean])(implicit messages: Messages): Option[SummaryListRow] =
    relevantAp12Months.map { answer =>
      val value = if (answer) "site.yes" else "site.no"

      SummaryListRowViewModel(
        key = Key("checkYourAnswers.relevantAp12Months.label"),
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel("site.change", routes.RelevantAp12MonthsController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(
              messages("checkYourAnswers.relevantAp12Months.label")
            )
        )
      )
    }

}
