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
import play.api.mvc._
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataHandlingError
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclAccountService, ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdHelper
import uk.gov.hmrc.economiccrimelevyreturns.views.html._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.economiccrimelevyreturns.controllers.BaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartAmendController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  eclAccountService: EclAccountService,
  returnsService: ReturnsService,
  sessionService: SessionService,
  noObligationForPeriodView: NoObligationForPeriodView,
  view: StartAmendView
)(implicit ex: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad(periodKey: String, returnNumber: String): Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)

    val startAmendUrl = routes.StartAmendController.onPageLoad(periodKey, returnNumber).url
    sessionService
      .upsert(
        SessionData(
          request.internalId,
          Map(SessionKeys.StartAmendUrl -> startAmendUrl)
        )
      )
      .flatMap { _ =>
        (for {
          obligationData    <- eclAccountService.retrieveObligationData.asResponseError
          obligationDetails <- processObligationDetails(obligationData, periodKey).asResponseError
        } yield obligationDetails)
          .fold(
            _ => Redirect(routes.NotableErrorController.answersAreInvalid()),
            {
              case Some(obligationData) =>
                Ok(
                  view(
                    returnNumber,
                    obligationData.inboundCorrespondenceFromDate,
                    obligationData.inboundCorrespondenceToDate,
                    Some(startAmendUrl)
                  )
                )
              case None                 => Ok(noObligationForPeriodView())
            }
          )
      }
  }

  private def validatePeriodKey(eclReturn: EclReturn, obligationDetails: ObligationDetails, periodKey: String)(implicit
    request: AuthorisedRequest[_]
  ): EitherT[Future, DataHandlingError, Unit] = {
    val optPeriodKey = eclReturn.obligationDetails.map(_.periodKey)
    if (optPeriodKey.contains(periodKey) || optPeriodKey.isEmpty) {
      val updatedReturn = eclReturn.copy(obligationDetails = Some(obligationDetails), returnType = Some(AmendReturn))
      returnsService.upsertReturn(updatedReturn)
    } else {
      for {
        _            <- returnsService.deleteReturn(request.internalId)
        updatedReturn = EclReturn
                          .empty(request.internalId, None)
                          .copy(obligationDetails = Some(obligationDetails), returnType = Some(AmendReturn))
        unit         <- returnsService.upsertReturn(updatedReturn)
      } yield unit
    }
  }

  private def processObligationDetails(obligationData: Option[ObligationData], periodKey: String)(implicit
    request: AuthorisedRequest[_]
  ): EitherT[Future, DataHandlingError, Option[ObligationDetails]] = {
    val obligationDetails =
      obligationData.flatMap(_.getObligationDetails(periodKey))

    obligationDetails match {
      case None                    => EitherT.rightT[Future, DataHandlingError](None)
      case Some(obligationDetails) =>
        for {
          eclReturn <- returnsService.getOrCreateReturn(request.internalId, Some(AmendReturn))
          _         <- validatePeriodKey(eclReturn, obligationDetails, periodKey)
        } yield Some(obligationDetails)
    }
  }
}
