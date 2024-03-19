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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, RequestHeader, Result}
import uk.gov.hmrc.economiccrimelevyreturns.cleanup.AmlRegulatedActivityDataCleanup
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction, StoreUrlAction}
import uk.gov.hmrc.economiccrimelevyreturns.forms.AmlRegulatedActivityFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, Mode}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclCalculatorService, ReturnsService}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdHelper
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AmlRegulatedActivityView, ErrorTemplate}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmlRegulatedActivityController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getReturnData: DataRetrievalAction,
  eclReturnsService: ReturnsService,
  eclLiabilityService: EclCalculatorService,
  formProvider: AmlRegulatedActivityFormProvider,
  dataCleanup: AmlRegulatedActivityDataCleanup,
  view: AmlRegulatedActivityView,
  storeUrl: StoreUrlAction
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with BaseController
    with ErrorHandler
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData andThen storeUrl) {
    implicit request =>
      Ok(view(form.prepare(request.eclReturn.carriedOutAmlRegulatedActivityForFullFy), mode, request.startAmendUrl))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, request.startAmendUrl))),
        carriedOutAmlRegulatedActivityForFullFy => {
          val answerChanged =
            !request.eclReturn.carriedOutAmlRegulatedActivityForFullFy.contains(carriedOutAmlRegulatedActivityForFullFy)
          val eclReturn     =
            dataCleanup.cleanup(
              request.eclReturn.copy(
                carriedOutAmlRegulatedActivityForFullFy = Some(carriedOutAmlRegulatedActivityForFullFy)
              )
            )

          (for {
            unit <- eclReturnsService.upsertReturn(eclReturn).asResponseError
          } yield unit)
            .foldF(
              err => Future.successful(routeError(err)),
              _ => route(eclReturn, carriedOutAmlRegulatedActivityForFullFy, mode, answerChanged)
            )
        }
      )
  }

  private def route(
    eclReturn: EclReturn,
    carriedOutAmlRegulatedActivityForFullFy: Boolean,
    mode: Mode,
    answerChanged: Boolean
  )(implicit
    requestHeader: Request[_]
  ): Future[Result] = if (answerChanged) {
    carriedOutAmlRegulatedActivityForFullFy match {
      case true  =>
        (for {
          calculatedLiability <- eclLiabilityService.calculateLiability(eclReturn).asResponseError
          calculatedReturn     = eclReturn.copy(calculatedLiability = Some(calculatedLiability))
          _                   <- eclReturnsService.upsertReturn(calculatedReturn).asResponseError
        } yield calculatedLiability).fold(
          error => routeError(error),
          _ => Redirect(routes.AmountDueController.onPageLoad(mode))
        )
      case false =>
        Future.successful(Redirect(eclReturn.amlRegulatedActivityLength match {
          case None => routes.AmlRegulatedActivityLengthController.onPageLoad(mode)
          case _    => routes.CheckYourAnswersController.onPageLoad()
        }))
    }
  } else {
    Future.successful(Redirect(if (eclReturn.hasContactInfo) {
      routes.CheckYourAnswersController.onPageLoad()
    } else {
      routes.AmountDueController.onPageLoad(CheckMode)
    }))
  }

}
