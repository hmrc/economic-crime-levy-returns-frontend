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

import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.economiccrimelevyreturns.connectors.{EclAccountConnector, ReturnsConnector}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.views.html._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartAmendController @Inject() (
                                       val controllerComponents: MessagesControllerComponents,
                                       authorise: AuthorisedAction,
                                       eclAccountConnector: EclAccountConnector,
                                       eclReturnsService: EclReturnsService,
                                       sessionService: SessionService,
                                       eclReturnsConnector: ReturnsConnector,
                                       noObligationForPeriodView: NoObligationForPeriodView,
                                       view: StartAmendView
)(implicit ex: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(periodKey: String, returnNumber: String): Action[AnyContent] = authorise.async { implicit request =>
    for {
      obligationData          <- eclAccountConnector.getObligations()
      eitherObligationDetails <- validatePeriodKey(obligationData, periodKey)
      _                        = eclReturnsService.upsertEclReturnType(request.internalId, AmendReturn)
    } yield eitherObligationDetails match {
      case Right(value) =>
        val startAmendUrl = routes.StartAmendController
          .onPageLoad(
            periodKey = periodKey,
            returnNumber = returnNumber
          )
          .url
        sessionService.upsert(
          SessionData(
            request.internalId,
            Map(SessionKeys.StartAmendUrl -> startAmendUrl)
          )
        )
        Ok(
          view(
            returnNumber,
            value.inboundCorrespondenceFromDate,
            value.inboundCorrespondenceToDate,
            Some(startAmendUrl)
          )
        )
      case Left(result) => result
    }
  }

  private def validatePeriodKey(obligationData: Option[ObligationData], periodKey: String)(implicit
    request: AuthorisedRequest[_]
  ): Future[Either[Result, ObligationDetails]] = {
    val obligationDetails =
      obligationData
        .map(_.obligations.flatMap(_.obligationDetails.find(_.periodKey == periodKey)))
        .flatMap(_.headOption)

    obligationDetails match {
      case None                    => Future.successful(Left(Ok(noObligationForPeriodView())))
      case Some(obligationDetails) =>
        for {
          eclReturn <- eclReturnsService.getOrCreateReturn(request.internalId, Some(AmendReturn))
          _         <- {
            val optPeriodKey = eclReturn.obligationDetails.map(_.periodKey)
            if (optPeriodKey.contains(periodKey) || optPeriodKey.isEmpty) {
              eclReturnsConnector.upsertReturn(eclReturn.copy(obligationDetails = Some(obligationDetails)))
            } else {
              eclReturnsConnector
                .deleteReturn(request.internalId)
                .map(_ =>
                  eclReturnsConnector.upsertReturn(
                    EclReturn.empty(request.internalId, None).copy(obligationDetails = Some(obligationDetails))
                  )
                )
            }
          }

        } yield Right(obligationDetails)
    }
  }
}
