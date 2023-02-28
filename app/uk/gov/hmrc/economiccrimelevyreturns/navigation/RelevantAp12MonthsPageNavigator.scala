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
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, NormalMode}

class RelevantAp12MonthsPageNavigator extends PageNavigator {

  override protected def navigateInNormalMode(eclReturn: EclReturn): Call =
    eclReturn.relevantAp12Months match {
      case Some(true)  => routes.UkRevenueController.onPageLoad(NormalMode)
      case Some(false) => routes.RelevantApLengthController.onPageLoad(NormalMode)
      case _           => routes.NotableErrorController.answersAreInvalid()
    }

  override protected def navigateInCheckMode(eclReturn: EclReturn): Call =
    eclReturn.relevantAp12Months match {
      case Some(true)  => routes.EstimatedEclAmountController.onPageLoad()
      case Some(false) => routes.RelevantApLengthController.onPageLoad(CheckMode)
      case _           => routes.NotableErrorController.answersAreInvalid()
    }

}
