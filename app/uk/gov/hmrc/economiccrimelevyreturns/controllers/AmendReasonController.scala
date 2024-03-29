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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyreturns.forms.AmendReasonFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, Mode}
import uk.gov.hmrc.economiccrimelevyreturns.navigation.AmendReasonPageNavigator
import uk.gov.hmrc.economiccrimelevyreturns.services.ReturnsService
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AmendReasonView, ErrorTemplate}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendReasonController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getReturnData: DataRetrievalAction,
  eclReturnsService: ReturnsService,
  formProvider: AmendReasonFormProvider,
  pageNavigator: AmendReasonPageNavigator,
  view: AmendReasonView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData) { implicit request =>
    getInboundCorrespondenceDates(request.eclReturn).fold(
      err => routeError(err),
      tuple => {
        val fromFy = tuple._1
        val toFy   = tuple._2

        Ok(
          view(
            form.prepare(request.eclReturn.amendReason),
            mode,
            fromFy,
            toFy,
            request.startAmendUrl
          )
        )
      }
    )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          getInboundCorrespondenceDates(request.eclReturn).fold(
            err => Future.successful(routeError(err)),
            tuple => {
              val fromFy = tuple._1
              val toFy   = tuple._2
              Future.successful(BadRequest(view(formWithErrors, mode, fromFy, toFy, request.startAmendUrl)))
            }
          ),
        reason => {
          val updatedReturn = request.eclReturn.copy(amendReason = Some(reason))
          (for {
            _ <- eclReturnsService
                   .upsertReturn(updatedReturn)
                   .asResponseError
          } yield updatedReturn).fold(
            err => routeError(err),
            eclReturn => Redirect(pageNavigator.nextPage(mode, eclReturn))
          )
        }
      )
  }

  private def getInboundCorrespondenceDates(eclReturn: EclReturn) =
    for {
      fromFinancialYear <- valueOrError(
                             eclReturn.obligationDetails.map(_.inboundCorrespondenceFromDate.getYear.toString),
                             "Obligation details from date"
                           )
      toFinancialYear   <-
        valueOrError(
          eclReturn.obligationDetails.map(_.inboundCorrespondenceToDate.getYear.toString),
          "Obligation details to date"
        )
    } yield (fromFinancialYear, toFinancialYear)

}
