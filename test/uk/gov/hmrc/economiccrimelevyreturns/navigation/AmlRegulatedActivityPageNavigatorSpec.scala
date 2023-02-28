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

import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, Mode}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._

import scala.concurrent.Future

class AmlRegulatedActivityPageNavigatorSpec extends SpecBase {

  val mockEclReturnsConnector: EclReturnsConnector = mock[EclReturnsConnector]

  val pageNavigator = new AmlRegulatedActivityPageNavigator(mockEclReturnsConnector)

  "nextPage" should {
    "return a Call to the amount of ECL you need to pay page from the AML regulated activity page in NormalMode when the 'Yes' option is selected" in forAll {
      eclReturn: EclReturn =>
        val updatedReturn = eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = Some(true))

      //TODO: Add routing call when next page is implemented
    }

    "return a Call to the check your answers page from the AML regulated activity page in CheckMode when the 'Yes' option is selected" in forAll {
      eclReturn: EclReturn =>
        val updatedReturn = eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = Some(true))

        await(
          pageNavigator.nextPage(CheckMode, updatedReturn)(fakeRequest)
        ) shouldBe routes.CheckYourAnswersController.onPageLoad()
    }

    "return a Call to the Aml regulated activity length page from the AML regulated activity page in either mode when the 'No' option is selected" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        val updatedReturn = eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = Some(false))

      //TODO: Add routing call when next page is implemented
    }
  }

}
