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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.economiccrimelevyreturns.cleanup.UkRevenueDataCleanup
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction, StoreUrlAction}
import uk.gov.hmrc.economiccrimelevyreturns.forms.FormImplicits._
import uk.gov.hmrc.economiccrimelevyreturns.forms.UkRevenueFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.models.Band.Small
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclCalculatorService, ReturnsService}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{ErrorTemplate, UkRevenueView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UkRevenueController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getReturnData: DataRetrievalAction,
  eclReturnsService: ReturnsService,
  eclLiabilityService: EclCalculatorService,
  formProvider: UkRevenueFormProvider,
  dataCleanup: UkRevenueDataCleanup,
  view: UkRevenueView,
  storeUrl: StoreUrlAction
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with BaseController
    with ErrorHandler
    with I18nSupport {

  val form: Form[BigDecimal] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData andThen storeUrl) {
    implicit request =>
      Ok(view(form.prepare(request.eclReturn.relevantApRevenue), mode, request.startAmendUrl))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, request.startAmendUrl))),
        revenue => {
          val answerChanged = !request.eclReturn.relevantApRevenue.contains(revenue)
          if (answerChanged) {
            val eclReturn = dataCleanup.cleanup(request.eclReturn.copy(relevantApRevenue = Some(revenue)))
            calculateLiability(mode, eclReturn)
          } else {
            Future.successful(Redirect(if (request.eclReturn.hasContactInfo) {
              routes.CheckYourAnswersController.onPageLoad()
            } else {
              routes.AmountDueController.onPageLoad(CheckMode)
            }))
          }
        }
      )
  }

  private def calculateLiability(mode: Mode, eclReturn: EclReturn)(implicit
    request: Request[_]
  ): Future[Result] =
    (for {
      _                   <- eclReturnsService.upsertReturn(eclReturn).asResponseError
      calculatedLiability <- eclLiabilityService.calculateLiability(eclReturn).asResponseError
      _                   <-
        eclReturnsService.upsertReturn(eclReturn.copy(calculatedLiability = Some(calculatedLiability))).asResponseError
    } yield calculatedLiability).foldF(
      error => Future.successful(routeError(error)),
      liability =>
        if (liability.calculatedBand == Small) {
          clearAmlActivityAnswersAndRecalculate(eclReturn, mode)
        } else {
          navigateLiable(eclReturn, mode)
        }
    )

  private def clearAmlActivityAnswersAndRecalculate(
    eclReturn: EclReturn,
    mode: Mode
  )(implicit request: Request[_]): Future[Result] = {
    val updatedReturn =
      eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = None, amlRegulatedActivityLength = None)
    (for {
      _         <- eclReturnsService.upsertReturn(updatedReturn).asResponseError
      liability <- eclLiabilityService.calculateLiability(updatedReturn).asResponseError
      _         <-
        eclReturnsService.upsertReturn(eclReturn.copy(calculatedLiability = Some(liability))).asResponseError
    } yield liability).fold(
      error => routeError(error),
      _ => Redirect(routes.AmountDueController.onPageLoad(mode))
    )
  }

  private def navigateLiable(eclReturn: EclReturn, mode: Mode): Future[Result] =
    Future.successful(Redirect(mode match {
      case NormalMode => routes.AmlRegulatedActivityController.onPageLoad(mode)
      case CheckMode  =>
        eclReturn.carriedOutAmlRegulatedActivityForFullFy match {
          case Some(_) => routes.AmountDueController.onPageLoad(mode)
          case None    => routes.AmlRegulatedActivityController.onPageLoad(mode)
        }
    }))

}
