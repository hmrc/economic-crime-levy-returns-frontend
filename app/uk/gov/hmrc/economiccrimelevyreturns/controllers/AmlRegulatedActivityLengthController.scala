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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.cleanup.AmlRegulatedActivityLengthDataCleanup
import uk.gov.hmrc.economiccrimelevyreturns.connectors.ReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyreturns.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyreturns.forms.AmlRegulatedActivityLengthFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.models.Mode
import uk.gov.hmrc.economiccrimelevyreturns.navigation.AmlRegulatedActivityLengthPageNavigator
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclLiabilityService, EclReturnsService}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdHelper
import uk.gov.hmrc.economiccrimelevyreturns.views.html.AmlRegulatedActivityLengthView
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmlRegulatedActivityLengthController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getReturnData: DataRetrievalAction,
  formProvider: AmlRegulatedActivityLengthFormProvider,
  pageNavigator: AmlRegulatedActivityLengthPageNavigator,
  view: AmlRegulatedActivityLengthView,
  eclLiabilityService: EclLiabilityService,
  eclReturnsService: EclReturnsService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with ErrorHandler {

  val form: Form[Int] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData) { implicit request =>
    Ok(view(form.prepare(request.eclReturn.amlRegulatedActivityLength), mode, request.startAmendUrl))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, request.startAmendUrl))),
        amlRegulatedActivityLength =>
          {
            var upsertedReturn = for {
            calculatedLiability <- eclLiabilityService.calculateLiability(request.eclReturn).asResponseError
            copiedReturn = request.eclReturn.copy(
              amlRegulatedActivityLength = Some(amlRegulatedActivityLength),
              calculatedLiability = Some(calculatedLiability))
            upsertedReturn <- eclReturnsService.upsertEclReturn(copiedReturn).asResponseError

          } yield upsertedReturn

            upsertedReturn.fold(
              error => ???,
              eclReturn => pageNavigator.nextPage(mode, eclReturn).map(Redirect)
            )
          }
      )
  }

}
