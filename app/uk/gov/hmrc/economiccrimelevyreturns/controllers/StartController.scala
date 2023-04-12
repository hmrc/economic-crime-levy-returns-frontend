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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.economiccrimelevyreturns.connectors.{EclAccountConnector, EclReturnsConnector}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyreturns.models.{Fulfilled, ObligationData, ObligationDetails, Open}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclReturnsService, EnrolmentStoreProxyService}
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyreturns.views.html.StartView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  eclAccountConnector: EclAccountConnector,
  eclReturnsService: EclReturnsService,
  eclReturnsConnector: EclReturnsConnector,
  view: StartView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(periodKey: String): Action[AnyContent] = authorise.async { implicit request =>
    for {
      registrationDate        <- enrolmentStoreProxyService.getEclRegistrationDate(request.eclRegistrationReference)
      obligationData          <- eclAccountConnector.getObligations()
      eitherObligationDetails <- validatePeriodKey(obligationData, periodKey)
    } yield eitherObligationDetails match {
      case Right(obligationDetails) =>
        Ok(
          view(
            request.eclRegistrationReference,
            ViewUtils.formatLocalDate(registrationDate),
            ViewUtils.formatObligationPeriodYears(obligationDetails)
          )
        )
      case Left(result)             => result
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
      case None                    => Future.successful(Left(Ok("No obligation details found for period"))) //TODO handle this properly
      case Some(obligationDetails) =>
        obligationDetails.status match {
          case Fulfilled =>
            Future.successful(
              Left(Ok(s"Obligation for period $periodKey already fulfilled"))
            ) //TODO handle this properly
          case Open      =>
            for {
              eclReturn <- eclReturnsService.getOrCreateReturn(request.internalId)
              _         <-
                eclReturnsConnector.upsertReturn(eclReturn.copy(obligationDetails = Some(obligationDetails)))
            } yield Right(obligationDetails)
        }
    }
  }

}
