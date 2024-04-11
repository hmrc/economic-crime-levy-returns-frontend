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

import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyreturns.models.{GetCorrespondenceAddressDetails, ObligationDetails, SessionKeys}
import uk.gov.hmrc.economiccrimelevyreturns.services.{RegistrationService, ReturnsService, SessionService}

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AmendReturnSubmittedView, ErrorTemplate}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendReturnSubmittedController @Inject() (
  appConfig: AppConfig,
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  registrationService: RegistrationService,
  view: AmendReturnSubmittedView,
  sessionService: SessionService,
  returnsService: ReturnsService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with ErrorHandler
    with BaseController {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    val eclReference = request.eclRegistrationReference
    (for {
      _                 <- returnsService.deleteReturn(request.internalId).asResponseError
      _                  = sessionService.delete(request.internalId).asResponseError
      obligationJson    <- valueOrErrorF(request.session.get(SessionKeys.obligationDetails), "Obligation details")
      obligationDetails <- valueOrErrorF(
                             Json.fromJson[ObligationDetails](Json.parse(obligationJson)).asOpt,
                             "Obligation details parsed from session"
                           )
      email             <- valueOrErrorF(request.session.get(SessionKeys.email), "Email")
      band              <- valueOrErrorF(request.session.get(SessionKeys.band), "Band")
      amount            <- valueOrErrorF(request.session.get(SessionKeys.amountDue), "Amount")
      isIncrease        <- valueOrErrorF(request.session.get(SessionKeys.isIncrease), "Increase")
      startAmendUrl      = request.session.get(SessionKeys.startAmendUrl)
    } yield ViewData(eclReference, obligationDetails, email, band, amount, isIncrease, startAmendUrl)).foldF(
      error => Future.successful(routeError(error)),
      viewData =>
        if (appConfig.getSubscriptionEnabled) {
          (for {
            subscription <- registrationService.getSubscription(eclReference).asResponseError
          } yield subscription).fold(
            err => routeError(err),
            subscription =>
              generateView(
                viewData.copy(
                  address = Some(subscription.correspondenceAddressDetails)
                )
              )
          )
        } else {
          Future.successful(generateView(viewData))
        }
    )
  }

  private def generateView(
    viewData: ViewData
  )(implicit
    request: Request[_],
    messages: Messages
  ): Result = {
    val obligationDetails = viewData.obligationDetails
    Ok(
      view(
        obligationDetails.inboundCorrespondenceFromDate,
        obligationDetails.inboundCorrespondenceToDate,
        viewData.email,
        viewData.address,
        viewData.eclReference,
        viewData.band,
        viewData.amount.toInt,
        viewData.isIncrease.toBoolean,
        viewData.startAmendUrl
      )(request, messages)
    )
  }
}

private case class ViewData(
  obligationDetails: ObligationDetails,
  address: Option[GetCorrespondenceAddressDetails],
  eclReference: String,
  email: String,
  band: String,
  amount: String,
  isIncrease: String,
  startAmendUrl: Option[String]
)

private object ViewData {
  def apply(
    eclReference: String,
    obligationDetails: ObligationDetails,
    email: String,
    band: String,
    amount: String,
    isIncrease: String,
    startAmendUrl: Option[String]
  ) =
    new ViewData(obligationDetails, None, eclReference, email, band, amount, isIncrease, startAmendUrl)
}
