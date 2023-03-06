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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn, Mode}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.services.EclLiabilityService

import scala.concurrent.Future

class AmlRegulatedActivityPageNavigatorSpec extends SpecBase {

  val mockEclLiabilityService: EclLiabilityService = mock[EclLiabilityService]

  val pageNavigator = new AmlRegulatedActivityPageNavigator(mockEclLiabilityService)

  "nextPage" should {
    "return a Call to the amount of ECL you need to pay page from the AML regulated activity page in either mode when the 'Yes' option is selected and the ECL return data is valid" in forAll {
      (eclReturn: EclReturn, mode: Mode, calculatedLiability: CalculatedLiability) =>
        val updatedReturn = eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = Some(true))

        when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(Some(Future.successful(updatedReturn.copy(calculatedLiability = Some(calculatedLiability)))))

        await(
          pageNavigator.nextPage(mode, updatedReturn)(fakeRequest)
        ) shouldBe routes.AmountDueController.onPageLoad(mode)
    }

    "return a Call to the answers are invalid page in either mode when the 'Yes' option is selected and the ECL return data is invalid" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        val updatedReturn =
          eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = Some(true), relevantAp12Months = None)

        when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(None)

        await(pageNavigator.nextPage(mode, updatedReturn)(fakeRequest)) shouldBe routes.NotableErrorController
          .answersAreInvalid()
    }

    "return a Call to the Aml regulated activity length page from the AML regulated activity page in either mode when the 'No' option is selected" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        val updatedReturn = eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = Some(false))

        await(
          pageNavigator.nextPage(mode, updatedReturn)(fakeRequest)
        ) shouldBe routes.AmlRegulatedActivityLengthController.onPageLoad(mode)
    }
  }

}
