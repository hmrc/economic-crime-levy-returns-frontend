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
import uk.gov.hmrc.economiccrimelevyreturns.cleanup.RelevantAp12MonthsDataCleanup
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction}
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
  view: RelevantAp12MonthsView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with BaseController
    with ErrorHandler
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData) { implicit request =>
    Ok(view(form.prepare(request.eclReturn.relevantAp12Months), mode, request.startAmendUrl))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, request.startAmendUrl))),
        relevantAp12Months => {
          val eclReturn = dataCleanup.cleanup(request.eclReturn.copy(relevantAp12Months = Some(relevantAp12Months)))
          eclReturnsService
            .upsertReturn(eclReturn)
            .asResponseError
            .foldF(
              error => Future.successful(routeError(error)),
              _ => navigateByMode(mode, relevantAp12Months, eclReturn).map(Redirect)
            )
        }
      )
  }

  private def navigateByMode(mode: Mode, relevantAp12Months: Boolean, eclReturn: EclReturn)(implicit
    request: RequestHeader
  ) =
    mode match {
      case NormalMode => navigateInNormalMode(relevantAp12Months)
      case CheckMode  => navigateInCheckMode(eclReturn, relevantAp12Months)
    }

  private def navigateInNormalMode(relevantAp12Months: Boolean): Future[Call] =
    relevantAp12Months match {
      case true  => Future.successful(routes.UkRevenueController.onPageLoad(NormalMode))
      case false => Future.successful(routes.RelevantApLengthController.onPageLoad(NormalMode))
    }

  private def navigateInCheckMode(eclReturn: EclReturn, relevantAp12Months: Boolean)(implicit request: RequestHeader) =
    relevantAp12Months match {
      case true  =>
        (for {
          calculatedLiability <- eclLiabilityService.calculateLiability(eclReturn).asResponseError
          _                   <-
            eclReturnsService
              .upsertReturn(eclReturn.copy(calculatedLiability = Some(calculatedLiability)))
              .asResponseError
        } yield calculatedLiability).foldF(
          error => Future.successful(routes.NotableErrorController.answersAreInvalid()),
          _ =>
            eclReturn.calculatedLiability match {
              case Some(calculatedLiability) if calculatedLiability.calculatedBand == Small =>
                clearAmlActivityAnswersAndRecalculate(eclReturn)
              case Some(_)                                                                  => navigateLiable(eclReturn)
              case _                                                                        => Future.successful(routes.NotableErrorController.answersAreInvalid())
            }
        )
      case false => Future.successful(routes.RelevantApLengthController.onPageLoad(CheckMode))
    }

  private def clearAmlActivityAnswersAndRecalculate(
    eclReturn: EclReturn
  )(implicit request: RequestHeader) = {
    val updatedReturn =
      eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = None, amlRegulatedActivityLength = None)
    (for {
      liability <- eclLiabilityService.calculateLiability(updatedReturn).asResponseError
      _         <- eclReturnsService.upsertReturn(updatedReturn.copy(calculatedLiability = Some(liability))).asResponseError
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
