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
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, SessionError}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclAccountService, EnrolmentStoreProxyService, ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdHelper
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyreturns.views.html._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  eclAccountService: EclAccountService,
  returnsService: ReturnsService,
  alreadySubmittedReturnView: AlreadySubmittedReturnView,
  noObligationForPeriodView: NoObligationForPeriodView,
  chooseReturnPeriodView: ChooseReturnPeriodView,
  view: StartView,
  sessionService: SessionService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def start(): Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)
    (for {
      eclReturn <- returnsService.getOrCreateReturn(request.internalId).asResponseError
    } yield eclReturn).fold(
      error => routeError(error),
      eclReturn =>
        eclReturn.obligationDetails match {
          case Some(obligationDetails) => Redirect(routes.StartController.onPageLoad(obligationDetails.periodKey))
          case None                    => Ok(chooseReturnPeriodView())
        }
    )
  }

  def onPageLoad(periodKey: String): Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)
    (for {
      _                 <- addPeriodKeyToSession(periodKey).asResponseError
      registrationDate  <-
        enrolmentStoreProxyService.getEclRegistrationDate(request.eclRegistrationReference).asResponseError
      obligationData    <- eclAccountService.retrieveObligationData.asResponseError
      obligationDetails <- processObligationDetails(obligationData, periodKey).asResponseError
    } yield (registrationDate, obligationDetails)).fold(
      error => routeError(error),
      {
        case (registrationDate, Some(obligationDetails)) =>
          obligationDetails.status match {
            case Fulfilled =>
              Ok(
                alreadySubmittedReturnView(
                  obligationDetails.inboundCorrespondenceFromDate.getYear.toString,
                  obligationDetails.inboundCorrespondenceToDate.getYear.toString,
                  ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceDateReceived.get)
                )
              )
            case Open      =>
              Ok(
                view(
                  request.eclRegistrationReference,
                  ViewUtils.formatLocalDate(registrationDate),
                  ViewUtils.formatObligationPeriodYears(obligationDetails),
                  None
                )
              )
          }
        case (_, None)                                   => Ok(noObligationForPeriodView())
      }
    )
  }

  def onSubmit(): Action[AnyContent] = authorise.async { implicit request =>
    (for {
      urlToReturnTo <-
        sessionService.getOptional(request.session, request.internalId, SessionKeys.UrlToReturnTo).asResponseError
    } yield urlToReturnTo).fold(
      err => routeError(err),
      urlToReturnTo =>
        Redirect(
          urlToReturnTo match {
            case Some(_) => routes.SavedResponsesController.onPageLoad()
            case None    => routes.RelevantAp12MonthsController.onPageLoad(NormalMode)
          }
        )
    )
  }

  private def addPeriodKeyToSession(periodKey: String)(implicit
    request: AuthorisedRequest[_]
  ): EitherT[Future, SessionError, Unit] =
    sessionService.upsert(
      SessionData(
        internalId = request.internalId,
        values = Map(SessionKeys.PeriodKey -> periodKey)
      )
    )

  private def processObligationDetails(obligationData: Option[ObligationData], periodKey: String)(implicit
    request: AuthorisedRequest[_]
  ): EitherT[Future, DataHandlingError, Option[ObligationDetails]] = {
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)

    val obligationDetails =
      obligationData.flatMap(_.getObligationDetails(periodKey))

    obligationDetails match {
      case None                    => EitherT[Future, DataHandlingError, Option[ObligationDetails]](Future.successful(Right(None)))
      case Some(obligationDetails) =>
        obligationDetails.status match {
          case Fulfilled =>
            if (obligationDetails.inboundCorrespondenceDateReceived.isEmpty) {
              EitherT[Future, DataHandlingError, Option[ObligationDetails]](
                Future.successful(
                  Left(
                    DataHandlingError.InternalUnexpectedError(
                      None,
                      Some("Fulfilled obligation does not have an inboundCorrespondenceDateReceived")
                    )
                  )
                )
              )
            } else {
              EitherT[Future, DataHandlingError, Option[ObligationDetails]](
                Future.successful(Right(Some(obligationDetails)))
              )
            }
          case Open      =>
            for {
              eclReturn <- returnsService.getOrCreateReturn(request.internalId, Some(FirstTimeReturn))
              _         <- validatePeriodKey(eclReturn, obligationDetails, periodKey)
            } yield Some(obligationDetails)
        }
    }
  }

  private def validatePeriodKey(eclReturn: EclReturn, obligationDetails: ObligationDetails, periodKey: String)(implicit
    request: AuthorisedRequest[_]
  ): EitherT[Future, DataHandlingError, Unit] = {
    val optPeriodKey = eclReturn.obligationDetails.map(_.periodKey)
    if (optPeriodKey.contains(periodKey) || optPeriodKey.isEmpty) {
      val updatedReturn =
        eclReturn.copy(obligationDetails = Some(obligationDetails), returnType = Some(FirstTimeReturn))
      returnsService.upsertReturn(updatedReturn)
    } else {
      for {
        _            <- returnsService.deleteReturn(request.internalId)
        updatedReturn = EclReturn
                          .empty(request.internalId, None)
                          .copy(obligationDetails = Some(obligationDetails), returnType = Some(FirstTimeReturn))
        unit         <- returnsService.upsertReturn(updatedReturn)
      } yield unit
    }
  }

}
