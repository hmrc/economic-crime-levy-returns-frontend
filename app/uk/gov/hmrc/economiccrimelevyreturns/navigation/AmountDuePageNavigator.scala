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
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode}

class AmountDuePageNavigator extends PageNavigator {

  override protected def navigateInNormalMode(eclReturn: EclReturn): Call =
    routes.ContactNameController.onPageLoad(NormalMode)

  override protected def navigateInCheckMode(eclReturn: EclReturn): Call =
    (
      eclReturn.contactName,
      eclReturn.contactRole,
      eclReturn.contactEmailAddress,
      eclReturn.contactTelephoneNumber
    ) match {
      case (Some(_), Some(_), Some(_), Some(_)) =>
        routes.CheckYourAnswersController.onPageLoad()
      case _                                    => routes.ContactNameController.onPageLoad(NormalMode)
    }

}
