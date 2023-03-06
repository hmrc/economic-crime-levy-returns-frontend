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
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyreturns.models.Mode
import uk.gov.hmrc.economiccrimelevyreturns.navigation.AmountDuePageNavigator
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers._
import uk.gov.hmrc.economiccrimelevyreturns.views.html.AmountDueView
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.govuk.summarylist._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AmountDueController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getReturnData: DataRetrievalAction,
  pageNavigator: AmountDuePageNavigator,
  view: AmountDueView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData) { implicit request =>
    val accountingDetails: SummaryList = SummaryListViewModel(
      rows = Seq(
        RelevantAp12MonthsSummary.row(),
        RelevantApLengthSummary.row(),
        UkRevenueSummary.row(),
        AmlRegulatedActivitySummary.row(),
        AmlRegulatedActivityLengthSummary.row()
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

    request.eclReturn.calculatedLiability match {
      case Some(calculatedLiability) => Ok(view(calculatedLiability, accountingDetails, mode))
      case _                         => Redirect(routes.NotableErrorController.answersAreInvalid())
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData) { implicit request =>
    Redirect(pageNavigator.nextPage(mode, request.eclReturn))
  }

}
