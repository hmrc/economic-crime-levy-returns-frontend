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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyreturns.models.{ObligationDetails, SessionKeys}

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.AmendReturnSubmittedView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

@Singleton
class AmendReturnSubmittedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  view: AmendReturnSubmittedView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = authorise { implicit request =>
    val obligationDetails: ObligationDetails = Json
      .parse(request.session(SessionKeys.ObligationDetails))
      .as[ObligationDetails]

    val fyStart      = obligationDetails.inboundCorrespondenceFromDate
    val fyEnd        = obligationDetails.inboundCorrespondenceToDate
    val contactEmail = request.session(SessionKeys.Email)

    Ok(view(fyStart = fyStart, fyEnd = fyEnd, confirmationEmail = contactEmail))
  }
}