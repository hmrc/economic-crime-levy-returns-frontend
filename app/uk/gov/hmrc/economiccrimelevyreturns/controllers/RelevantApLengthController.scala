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
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, RequestHeader}
import uk.gov.hmrc.economiccrimelevyreturns.cleanup.RelevantApLengthDataCleanup
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction, StoreUrlAction}
import uk.gov.hmrc.economiccrimelevyreturns.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyreturns.forms.RelevantApLengthFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.models.Band.Small
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclCalculatorService, ReturnsService}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdHelper
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{ErrorTemplate, RelevantApLengthView}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RelevantApLengthController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getReturnData: DataRetrievalAction,
  eclReturnsService: ReturnsService,
  eclLiabilityService: EclCalculatorService,
  formProvider: RelevantApLengthFormProvider,
  dataCleanup: RelevantApLengthDataCleanup,
  view: RelevantApLengthView,
  storeUrl: StoreUrlAction
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with BaseController
    with ErrorHandler
    with I18nSupport {

  val form: Form[Int] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData andThen storeUrl) {
    implicit request =>
      Ok(view(form.prepare(request.eclReturn.relevantApLength), mode, request.startAmendUrl))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, request.startAmendUrl))),
        relevantApLength => {
          val eclReturn = dataCleanup.cleanup(request.eclReturn.copy(relevantApLength = Some(relevantApLength)))
          eclReturnsService
            .upsertReturn(eclReturn)
            .asResponseError
            .foldF(
              error => Future.successful(routeError(error)),
              _ => navigateByMode(mode, eclReturn).map(Redirect)
            )
        }
      )
  }

  private def navigateByMode(mode: Mode, eclReturn: EclReturn)(implicit request: RequestHeader) =
    mode match {
      case NormalMode => Future.successful(routes.UkRevenueController.onPageLoad(NormalMode))
      case CheckMode  => navigateInCheckMode(eclReturn)
    }

  private def navigateInCheckMode(eclReturn: EclReturn)(implicit request: RequestHeader) =
    (for {
      calculatedLiability <- eclLiabilityService.calculateLiability(eclReturn).asResponseError
      updatedReturn        = eclReturn.copy(calculatedLiability = Some(calculatedLiability))
      _                   <- eclReturnsService.upsertReturn(updatedReturn).asResponseError
    } yield updatedReturn).foldF(
      error => Future.successful(routes.NotableErrorController.answersAreInvalid()),
      calculatedReturn =>
        calculatedReturn.calculatedLiability match {
          case Some(calculatedLiability) if calculatedLiability.calculatedBand == Small =>
            clearAmlActivityAnswersAndRecalculate(eclReturn)
          case Some(_)                                                                  => navigateLiable(calculatedReturn)
          case _                                                                        => Future.successful(routes.NotableErrorController.answersAreInvalid())
        }
    )

  private def clearAmlActivityAnswersAndRecalculate(
    eclReturn: EclReturn
  )(implicit request: RequestHeader) = {
    val updatedReturn =
      eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = None, amlRegulatedActivityLength = None)
    (for {
      _               <- eclReturnsService.upsertReturn(updatedReturn).asResponseError
      liability       <- eclLiabilityService.calculateLiability(updatedReturn).asResponseError
      calculatedReturn = updatedReturn.copy(calculatedLiability = Some(liability))
      _               <- eclReturnsService.upsertReturn(calculatedReturn).asResponseError
    } yield liability).fold(
      error => routes.NotableErrorController.answersAreInvalid(),
      _ => routes.AmountDueController.onPageLoad(CheckMode)
    )
  }

  private def navigateLiable(eclReturn: EclReturn): Future[Call] =
    eclReturn.carriedOutAmlRegulatedActivityForFullFy match {
      case Some(_) => Future.successful(routes.AmountDueController.onPageLoad(CheckMode))
      case None    => Future.successful(routes.AmlRegulatedActivityController.onPageLoad(CheckMode))
    }

}
