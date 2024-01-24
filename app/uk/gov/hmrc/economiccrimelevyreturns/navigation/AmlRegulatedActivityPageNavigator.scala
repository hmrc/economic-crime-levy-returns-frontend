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

import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, Mode, NormalMode}

class AmlRegulatedActivityPageNavigator extends PageNavigator {

  override protected def navigateInNormalMode(
    eclReturn: EclReturn
  ): Call = navigate(NormalMode, eclReturn)

  override protected def navigateInCheckMode(
    eclReturn: EclReturn
  ): Call = navigate(CheckMode, eclReturn)

  private def navigate(mode: Mode, eclReturn: EclReturn): Call =
    eclReturn.carriedOutAmlRegulatedActivityForFullFy match {
      case Some(true)  => routes.AmountDueController.onPageLoad(mode)
      case Some(false) => routes.AmlRegulatedActivityLengthController.onPageLoad(mode)
      case None        => routes.NotableErrorController.answersAreInvalid()
    }
}
