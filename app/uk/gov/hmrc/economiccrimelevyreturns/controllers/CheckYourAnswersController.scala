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

package uk.gov.hmrc.economiccrimelevyreturns.controllers

import com.google.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction, ValidatedReturnAction}
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers._
import uk.gov.hmrc.economiccrimelevyreturns.views.html.CheckYourAnswersView
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.govuk.summarylist._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Singleton

@Singleton
class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedAction,
  getReturnData: DataRetrievalAction,
  validateReturnData: ValidatedReturnAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (authorise andThen getReturnData andThen validateReturnData) {
    implicit request =>
      val eclDetails: SummaryList = SummaryListViewModel(
        rows = Seq(
          EclReferenceNumberSummary.row(),
          RelevantAp12MonthsSummary.row(),
          RelevantApLengthSummary.row(),
          UkRevenueSummary.row(),
          AmlRegulatedActivitySummary.row(),
          AmlRegulatedActivityLengthSummary.row(),
          CalculatedBandSummary.row(),
          AmountDueSummary.row()
        ).flatten
      ).withCssClass("govuk-!-margin-bottom-9")

      val contactDetails: SummaryList = SummaryListViewModel(
        rows = Seq(
          ContactNameSummary.row(),
          ContactRoleSummary.row(),
          ContactEmailSummary.row(),
          ContactNumberSummary.row()
        ).flatten
      ).withCssClass("govuk-!-margin-bottom-9")

      Ok(view(eclDetails, contactDetails))
  }

  def onSubmit: Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    ???
  }

}
