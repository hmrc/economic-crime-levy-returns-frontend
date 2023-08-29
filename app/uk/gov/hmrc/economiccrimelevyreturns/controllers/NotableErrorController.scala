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
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AgentCannotSubmitReturnView, AnswersAreInvalidView, NotRegisteredView}
import uk.gov.hmrc.http.HttpVerbs.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class NotableErrorController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getReturnData: DataRetrievalAction,
  answersAreInvalidView: AnswersAreInvalidView,
  notRegisteredView: NotRegisteredView,
  agentCannotSubmitReturnView: AgentCannotSubmitReturnView,
  appConfig: AppConfig
) extends FrontendBaseController
    with I18nSupport {

  def answersAreInvalid: Action[AnyContent] = (authorise andThen getReturnData) { implicit request =>
    Ok(answersAreInvalidView())
  }

  def notRegistered: Action[AnyContent] = Action { implicit request =>
    Redirect(Call(GET, s"${appConfig.eclEnrolmentBaseUrl}/add-economic-crime-levy/do-you-have-an-ecl-reference-number"))
  }

  def agentCannotSubmitReturn: Action[AnyContent] = Action { implicit request =>
    Ok(agentCannotSubmitReturnView())
  }
}
