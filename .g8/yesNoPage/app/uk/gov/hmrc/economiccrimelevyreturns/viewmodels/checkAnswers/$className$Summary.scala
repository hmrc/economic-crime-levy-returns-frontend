package uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkAnswers

import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, Return}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.implicits._

object $className$Summary  {

  def row(eclReturn: Return)(implicit messages: Messages): Option[SummaryListRow] =
    eclReturn.???.map { //TODO Choose the data you want
      answer =>
        val value = if (answer) "site.yes" else "site.no"

        SummaryListRowViewModel(
          key     = "$className;format="decap"$.checkYourAnswersLabel",
          value   = ValueViewModel(value),
          actions = Seq(
            ActionItemViewModel("site.change", routes.$className$Controller.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("$className;format="decap"$.change.hidden"))
          )
        )
    }
}
