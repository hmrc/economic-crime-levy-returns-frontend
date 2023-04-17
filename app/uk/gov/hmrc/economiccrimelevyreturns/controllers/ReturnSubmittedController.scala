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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionKeys
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyreturns.views.html.ReturnSubmittedView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}

@Singleton
class ReturnSubmittedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  view: ReturnSubmittedView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = authorise { implicit request =>
    val chargeReference: String = request.session
      .get(SessionKeys.ChargeReference)
      .getOrElse(throw new IllegalStateException("Charge reference number not found in session"))

    val amountDue: BigDecimal = BigDecimal(
      request.session
        .get(SessionKeys.AmountDue)
        .getOrElse(throw new IllegalStateException("Amount due not found in session"))
    )

    Ok(view(chargeReference, ViewUtils.formatToday(), amountDue))
  }

}
