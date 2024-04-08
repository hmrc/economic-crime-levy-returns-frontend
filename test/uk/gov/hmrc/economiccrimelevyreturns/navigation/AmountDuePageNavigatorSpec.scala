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
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, FirstTimeReturn, NormalMode}

class AmountDuePageNavigatorSpec extends SpecBase {

  val pageNavigator = new AmountDuePageNavigator()

  "nextPage" should {
    "return a Call to the contact name page in NormalMode" in forAll { eclReturn: EclReturn =>
      val updatedReturn: EclReturn = eclReturn.copy(contactName = None)

      pageNavigator.nextPage(NormalMode, updatedReturn) shouldBe routes.ContactNameController.onPageLoad(NormalMode)
    }

    "return a Call to the contact name page in CheckMode if any of the contact details are absent from the ECL return" in forAll {
      eclReturn: EclReturn =>
        val updatedReturn: EclReturn = eclReturn.copy(contactName = None)

        pageNavigator.nextPage(CheckMode, updatedReturn) shouldBe routes.ContactNameController.onPageLoad(NormalMode)
    }

    "return a Call to the check your answers page in CheckMode if all the contact details are present in the ECL return" in forAll {
      (eclReturn: EclReturn, name: String, role: String, email: String, number: String) =>
        val updatedReturn: EclReturn = eclReturn.copy(
          contactName = Some(name),
          contactRole = Some(role),
          contactEmailAddress = Some(email),
          contactTelephoneNumber = Some(number)
        )

        pageNavigator.nextPage(CheckMode, updatedReturn) shouldBe routes.CheckYourAnswersController.onPageLoad(
          eclReturn.returnType.getOrElse(FirstTimeReturn)
        )
    }
  }

}
