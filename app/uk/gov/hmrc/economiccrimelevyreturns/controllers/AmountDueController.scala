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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction, StoreUrlAction}
import uk.gov.hmrc.economiccrimelevyreturns.models.Mode
import uk.gov.hmrc.economiccrimelevyreturns.navigation.AmountDuePageNavigator
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers._
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyreturns.views.html.AmountDueView
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}

@Singleton
class AmountDueController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getReturnData: DataRetrievalAction,
  pageNavigator: AmountDuePageNavigator,
  view: AmountDueView,
  storeUrl: StoreUrlAction
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData andThen storeUrl) {
    implicit request =>
      val eclReturn                      = request.eclReturn
      val accountingDetails: SummaryList = SummaryListViewModel(
        rows = Seq(
          RelevantAp12MonthsSummary.row(eclReturn.relevantAp12Months),
          RelevantApLengthSummary.row(eclReturn.relevantApLength),
          UkRevenueSummary.row(eclReturn.relevantApRevenue),
          AmlRegulatedActivitySummary.row(eclReturn.carriedOutAmlRegulatedActivityForFullFy),
          AmlRegulatedActivityLengthSummary.row(eclReturn.amlRegulatedActivityLength)
        ).flatten
      ).withCssClass("govuk-!-margin-bottom-9")

      (request.eclReturn.obligationDetails, request.eclReturn.calculatedLiability) match {
        case (Some(obligationDetails), Some(calculatedLiability)) =>
          Ok(
            view(
              ViewUtils.formatObligationPeriodYears(obligationDetails),
              calculatedLiability,
              accountingDetails,
              mode,
              request.startAmendUrl
            )
          )
        case _                                                    => Redirect(routes.NotableErrorController.answersAreInvalid())
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData) { implicit request =>
    Redirect(pageNavigator.nextPage(mode, request.eclReturn))
  }

}
