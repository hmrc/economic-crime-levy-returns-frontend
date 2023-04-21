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

import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, NormalMode}

class ContactNamePageNavigatorSpec extends SpecBase {

  val pageNavigator = new ContactNamePageNavigator()

  "nextPage" should {
    "return a Call to the contact role page in NormalMode" in forAll { (eclReturn: EclReturn, name: String) =>
      val updatedReturn: EclReturn = eclReturn.copy(contactName = Some(name))

      pageNavigator.nextPage(NormalMode, updatedReturn) shouldBe routes.ContactRoleController.onPageLoad(NormalMode)
    }

    "return a Call to the check your answers page in CheckMode" in forAll { (eclReturn: EclReturn, name: String) =>
      val updatedReturn: EclReturn = eclReturn.copy(contactName = Some(name))

      pageNavigator.nextPage(CheckMode, updatedReturn) shouldBe routes.CheckYourAnswersController.onPageLoad()
    }
  }

}
