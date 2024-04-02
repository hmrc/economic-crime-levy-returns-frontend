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
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{ResponseError, SessionError}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EnrolmentStoreProxyService, ReturnsService, SessionService}
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
  getReturnData: DataRetrievalAction,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
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
          case Some(obligationDetails) =>
            Redirect(routes.StartController.onPageLoad(obligationDetails.periodKey))
              .withSession(addToSession(Seq(SessionKeys.periodKey -> obligationDetails.periodKey)))
          case None                    =>
            request.session.get(SessionKeys.periodKey) match {
              case Some(periodKey) =>
                Redirect(routes.StartController.onPageLoad(periodKey))
              case None            =>
                Ok(chooseReturnPeriodView())
            }
        }
    )
  }

  def onPageLoad(periodKey: String): Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)
    (for {
      _                <- addPeriodKeyToSessionTable(periodKey, request.internalId).asResponseError
      registrationDate <-
        enrolmentStoreProxyService.getEclRegistrationDate(request.eclRegistrationReference).asResponseError
    } yield (registrationDate, request.eclReturn.obligationDetails)).fold(
      error => routeError(error),
      {
        case (registrationDate, Some(obligationDetails)) =>
          obligationDetails.status match {
            case Fulfilled =>
              if (obligationDetails.inboundCorrespondenceDateReceived.isEmpty) {
                routeError(ResponseError.internalServiceError("Missing inboundCorrespondenceDateReceived"))
              } else {
                Ok(
                  alreadySubmittedReturnView(
                    obligationDetails.inboundCorrespondenceFromDate.getYear.toString,
                    obligationDetails.inboundCorrespondenceToDate.getYear.toString,
                    ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceDateReceived.get)
                  )
                )
              }
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

  def onSubmit(): Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    (for {
      urlToReturnTo <-
        sessionService.getOptional(request.session, request.internalId, SessionKeys.urlToReturnTo).asResponseError
      periodKey     <- valueOrErrorF(request.periodKey, "Period key")
    } yield (urlToReturnTo, periodKey)).fold(
      err => routeError(err),
      tuple => {
        val urlToReturnTo = tuple._1
        val periodKey     = tuple._2
        Redirect(
          urlToReturnTo match {
            case Some(_) => routes.SavedResponsesController.onPageLoad()
            case None    => routes.RelevantAp12MonthsController.onPageLoad(NormalMode)
          }
        ).withSession(addToSession(Seq(SessionKeys.periodKey -> periodKey)))
      }
    )
  }

  private def addPeriodKeyToSessionTable(periodKey: String, internalId: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, SessionError, Unit] =
    sessionService.upsert {
      SessionData(
        internalId = internalId,
        values = Map(SessionKeys.periodKey -> periodKey)
      )
    }

}
