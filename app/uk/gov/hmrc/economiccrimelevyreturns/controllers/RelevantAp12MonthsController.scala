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
import uk.gov.hmrc.economiccrimelevyreturns.cleanup.RelevantAp12MonthsDataCleanup
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction, StoreUrlAction}
import uk.gov.hmrc.economiccrimelevyreturns.forms.FormImplicits._
import uk.gov.hmrc.economiccrimelevyreturns.forms.RelevantAp12MonthsFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.models.Band.Small
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclCalculatorService, ReturnsService}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdHelper
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{ErrorTemplate, RelevantAp12MonthsView}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RelevantAp12MonthsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getReturnData: DataRetrievalAction,
  eclReturnsService: ReturnsService,
  eclLiabilityService: EclCalculatorService,
  formProvider: RelevantAp12MonthsFormProvider,
  dataCleanup: RelevantAp12MonthsDataCleanup,
  view: RelevantAp12MonthsView,
  storeUrl: StoreUrlAction
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with BaseController
    with ErrorHandler
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData andThen storeUrl) {
    implicit request =>
      Ok(view(form.prepare(request.eclReturn.relevantAp12Months), mode, request.startAmendUrl))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, request.startAmendUrl))),
        relevantAp12Months => {
          val answerChanged = !request.eclReturn.relevantAp12Months.contains(relevantAp12Months)
          val eclReturn     = dataCleanup.cleanup(request.eclReturn.copy(relevantAp12Months = Some(relevantAp12Months)))
          eclReturnsService
            .upsertReturn(eclReturn)
            .asResponseError
            .foldF(
              error => Future.successful(routeError(error)),
              _ => navigateByMode(mode, relevantAp12Months, eclReturn, answerChanged)
            )
        }
      )
  }

  private def navigateByMode(mode: Mode, relevantAp12Months: Boolean, eclReturn: EclReturn, answerChanged: Boolean)(
    implicit request: Request[_]
  ): Future[Result] =
    mode match {
      case NormalMode => navigateInNormalMode(relevantAp12Months)
      case CheckMode  => navigateInCheckMode(eclReturn, relevantAp12Months, answerChanged)
    }

  private def navigateInNormalMode(relevantAp12Months: Boolean): Future[Result] =
    Future.successful(Redirect(relevantAp12Months match {
      case true  => routes.UkRevenueController.onPageLoad(NormalMode)
      case false => routes.RelevantApLengthController.onPageLoad(NormalMode)
    }))

  private def navigateInCheckMode(eclReturn: EclReturn, relevantAp12Months: Boolean, answerChanged: Boolean)(implicit
    request: Request[_]
  ): Future[Result] = if (answerChanged) {
    relevantAp12Months match {
      case true  =>
        (for {
          calculatedLiability <- eclLiabilityService.calculateLiability(eclReturn).asResponseError
          updatedReturn        = eclReturn.copy(calculatedLiability = Some(calculatedLiability))
          _                   <- eclReturnsService.upsertReturn(updatedReturn).asResponseError
        } yield updatedReturn).foldF(
          error => Future.successful(routeError(error)),
          updatedReturn =>
            updatedReturn.calculatedLiability match {
              case Some(calculatedLiability) if calculatedLiability.calculatedBand == Small =>
                clearAmlActivityAnswersAndRecalculate(updatedReturn)
              case Some(_)                                                                  =>
                navigateLiable(updatedReturn)
              case _                                                                        =>
                Future.successful(Redirect(routes.NotableErrorController.answersAreInvalid()))
            }
        )
      case false =>
        Future.successful(Redirect(routes.RelevantApLengthController.onPageLoad(CheckMode)))
    }
  } else {
    Future.successful(Redirect(if (eclReturn.hasContactInfo) {
      routes.CheckYourAnswersController.onPageLoad()
    } else {
      routes.AmountDueController.onPageLoad(CheckMode)
    }))
  }

  private def clearAmlActivityAnswersAndRecalculate(
    eclReturn: EclReturn
  )(implicit request: Request[_]): Future[Result] = {
    val updatedReturn =
      eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = None, amlRegulatedActivityLength = None)
    (for {
      liability <- eclLiabilityService.calculateLiability(updatedReturn).asResponseError
      _         <- eclReturnsService.upsertReturn(updatedReturn.copy(calculatedLiability = Some(liability))).asResponseError
    } yield liability).fold(
      error => routeError(error),
      _ => Redirect(routes.AmountDueController.onPageLoad(CheckMode))
    )
  }

  private def navigateLiable(eclReturn: EclReturn): Future[Result] =
    Future.successful(Redirect(eclReturn.carriedOutAmlRegulatedActivityForFullFy match {
      case Some(_) => routes.AmountDueController.onPageLoad(CheckMode)
      case None    => routes.AmlRegulatedActivityController.onPageLoad(CheckMode)
    }))

}
