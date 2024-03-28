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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError
import uk.gov.hmrc.economiccrimelevyreturns.models.{ObligationDetails, SessionKeys}
import uk.gov.hmrc.economiccrimelevyreturns.services.{ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{ErrorTemplate, NilReturnSubmittedView, ReturnSubmittedView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnSubmittedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  returnSubmittedView: ReturnSubmittedView,
  nilReturnSubmittedView: NilReturnSubmittedView,
  returnsService: ReturnsService,
  sessionService: SessionService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with BaseController
    with I18nSupport
    with ErrorHandler {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    val chargeReference: Option[String] = request.session.get(SessionKeys.chargeReference)
    val obligationDetails               = ObligationDetails.read(request.session.get(SessionKeys.obligationDetails))
    val email                           = request.session.get(SessionKeys.email)

    (for {
      amountDueFromSession <- valueOrErrorF(request.session.get(SessionKeys.amountDue), "amount due")
      amountDue             = BigDecimal(amountDueFromSession)
      _                    <- returnsService.deleteReturn(request.internalId).asResponseError
      _                     = sessionService.delete(request.internalId).asResponseError
      obligation           <- valueOrError(obligationDetails)
      email                <- valueOrError(email)
    } yield (obligation, email, amountDue)).foldF(
      error => Future.successful(routeError(error)),
      response => {
        val obligation = response._1
        val email      = response._2
        chargeReference match {
          case Some(c) =>
            Future.successful(
              Ok(
                returnSubmittedView(
                  c,
                  ViewUtils.formatToday(),
                  ViewUtils.formatLocalDate(obligation.inboundCorrespondenceDueDate),
                  obligation.inboundCorrespondenceFromDate.getYear.toString,
                  obligation.inboundCorrespondenceToDate.getYear.toString,
                  response._3,
                  email
                )
              )
            )
          case None    =>
            Future.successful(
              Ok(
                nilReturnSubmittedView(
                  ViewUtils.formatToday(),
                  obligation.inboundCorrespondenceFromDate.getYear.toString,
                  obligation.inboundCorrespondenceToDate.getYear.toString,
                  response._3,
                  email
                )
              )
            )
        }
      }
    )
  }

  private def valueOrError[T](value: Option[T]) =
    EitherT(Future.successful(value.map(Right(_)).getOrElse(Left(ResponseError.internalServiceError()))))

}
