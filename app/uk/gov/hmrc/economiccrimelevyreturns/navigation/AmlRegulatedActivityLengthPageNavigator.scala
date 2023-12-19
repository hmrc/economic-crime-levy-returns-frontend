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

package uk.gov.hmrc.economiccrimelevyreturns.navigation

import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.Future

class AmlRegulatedActivityLengthPageNavigator @Inject() extends AsyncPageNavigator with FrontendHeaderCarrierProvider {

  override protected def navigateInNormalMode(
    eclReturn: EclReturn
  )(implicit request: RequestHeader): Future[Call] =
    Future.successful(routes.AmountDueController.onPageLoad(NormalMode))

  override protected def navigateInCheckMode(
    eclReturn: EclReturn
  )(implicit request: RequestHeader): Future[Call] =
    Future.successful(routes.CheckYourAnswersController.onPageLoad())
}
