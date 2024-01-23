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
import uk.gov.hmrc.economiccrimelevyreturns.cleanup.RelevantAp12MonthsDataCleanup
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyreturns.forms.FormImplicits._
import uk.gov.hmrc.economiccrimelevyreturns.forms.{CancelReturnAmendmentFormProvider, RelevantAp12MonthsFormProvider}
import uk.gov.hmrc.economiccrimelevyreturns.models.{Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.navigation.RelevantAp12MonthsPageNavigator
import uk.gov.hmrc.economiccrimelevyreturns.services.EclReturnsService
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{CancelReturnAmendmentView, RelevantAp12MonthsView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CancelReturnAmendmentController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getReturnData: DataRetrievalAction,
  eclReturnsService: EclReturnsService,
  formProvider: CancelReturnAmendmentFormProvider,
  appConfig: AppConfig,
  view: CancelReturnAmendmentView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (authorise andThen getReturnData) { implicit request =>
    Ok(view(form.prepare(None), request.startAmendUrl))
  }

  def onSubmit(): Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, request.startAmendUrl))),
        cancelReturnAmendment =>
          if (cancelReturnAmendment) {
            eclReturnsService.deleteEclReturn(request.eclReturn.internalId).map(_ => Redirect(appConfig.eclAccountUrl))
          } else {
            Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad()))
          }
      )
  }
}
