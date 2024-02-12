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

import cats.data.EitherT

import java.time.LocalDate
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError
import uk.gov.hmrc.economiccrimelevyreturns.models.{GetCorrespondenceAddressDetails, ObligationDetails, SessionKeys}
import uk.gov.hmrc.economiccrimelevyreturns.services.{RegistrationService, ReturnsService, SessionService}

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AmendReturnSubmittedView, ErrorTemplate}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendReturnSubmittedController @Inject() (
  appConfig: AppConfig,
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getReturnData: DataRetrievalAction,
  registrationService: RegistrationService,
  view: AmendReturnSubmittedView,
  sessionService: SessionService,
  returnsService: ReturnsService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with ErrorHandler
    with BaseController {

  def onPageLoad: Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    val eclReference      = request.eclRegistrationReference
    val obligationDetails = request.eclReturn.obligationDetails
    val email             = request.eclReturn.contactEmailAddress

    (for {
      _          <- returnsService.deleteReturn(request.internalId).asResponseError
      _           = sessionService.delete(request.internalId)
      obligation <- valueOrError(obligationDetails)
      email      <- valueOrError(email)
    } yield (obligation, email)).foldF(
      error => {
        println(error)
        Future.successful(routeError(error))
      },
      obligationAndEmail => {
        val obligation = obligationAndEmail._1
        val email      = obligationAndEmail._2
        if (appConfig.getSubscriptionEnabled) {
          (for {
            subscription <- registrationService.getSubscription(eclReference).asResponseError
          } yield subscription).fold(
            err => routeError(err),
            subscription =>
              generateView(obligation, Some(subscription.correspondenceAddressDetails), eclReference, email)
          )
        } else {
          Future.successful(generateView(obligation, None, eclReference, email))
        }
      }
    )
  }

  private def valueOrError[T](value: Option[T]) =
    EitherT(Future.successful(value.map(Right(_)).getOrElse(Left(ResponseError.internalServiceError()))))

  def generateView(
    obligationDetails: ObligationDetails,
    address: Option[GetCorrespondenceAddressDetails],
    eclReference: String,
    email: String
  )(implicit request: Request[_], messages: Messages): Result =
    Ok(
      view(
        fyStart = obligationDetails.inboundCorrespondenceFromDate,
        fyEnd = obligationDetails.inboundCorrespondenceToDate,
        confirmationEmail = email,
        contactAddress = if (address.isEmpty) None else address,
        eclReference
      )(request, messages)
    )

}
