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
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, CheckMode, EclReturn, Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.services.EclLiabilityService

import scala.concurrent.Future

class RelevantAp12MonthsPageNavigatorSpec extends SpecBase {

  val mockEclLiabilityService: EclLiabilityService = mock[EclLiabilityService]

  val pageNavigator = new RelevantAp12MonthsPageNavigator(mockEclLiabilityService)

  "nextPage" should {
    "return a Call to the UK revenue page from the relevant AP 12 months page in NormalMode when the 'Yes' option is selected" in forAll {
      eclReturn: EclReturn =>
        val updatedReturn = eclReturn.copy(relevantAp12Months = Some(true))

        await(pageNavigator.nextPage(NormalMode, updatedReturn)(fakeRequest)) shouldBe routes.UkRevenueController
          .onPageLoad(NormalMode)
    }

    "return a Call to the ECL amount due page from the relevant AP 12 months page in CheckMode when the 'Yes' option is selected and the ECL return data is valid" in forAll {
      (eclReturn: EclReturn, calculatedLiability: CalculatedLiability) =>
        val updatedReturn = eclReturn.copy(relevantAp12Months = Some(true))

        when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(Some(Future.successful(updatedReturn.copy(calculatedLiability = Some(calculatedLiability)))))

        await(
          pageNavigator.nextPage(CheckMode, updatedReturn)(fakeRequest)
        ) shouldBe routes.AmountDueController.onPageLoad()
    }

    "return a Call to the answers are invalid page in CheckMode when the 'Yes' option is selected and the ECL return data is invalid" in forAll {
      (eclReturn: EclReturn) =>
        val updatedReturn =
          eclReturn.copy(relevantAp12Months = Some(true), carriedOutAmlRegulatedActivityForFullFy = None)

        when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(None)

        await(pageNavigator.nextPage(CheckMode, updatedReturn)(fakeRequest)) shouldBe routes.NotableErrorController
          .answersAreInvalid()
    }

    "return a Call to the relevant AP length page from the relevant AP 12 months page in either mode when the 'No' option is selected" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        val updatedReturn = eclReturn.copy(relevantAp12Months = Some(false))

        await(pageNavigator.nextPage(mode, updatedReturn)(fakeRequest)) shouldBe routes.RelevantApLengthController
          .onPageLoad(mode)
    }
  }

}
