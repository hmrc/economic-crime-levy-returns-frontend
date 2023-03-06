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

class UkRevenuePageNavigatorSpec extends SpecBase {

  val mockEclLiabilityService: EclLiabilityService = mock[EclLiabilityService]

  val pageNavigator = new UkRevenuePageNavigator(mockEclLiabilityService)

  "nextPage" should {
    "return a Call to the Aml regulated activity for full financial year page in NormalMode" in forAll {
      (eclReturn: EclReturn, ukRevenue: Long) =>
        val updatedReturn = eclReturn.copy(relevantApRevenue = Some(ukRevenue))

        await(
          pageNavigator.nextPage(NormalMode, updatedReturn)(fakeRequest)
        ) shouldBe routes.AmlRegulatedActivityController.onPageLoad(
          NormalMode
        )
    }
    "return a Call to the ECL amount due page in CheckMode and the ECL return data is valid" in forAll {
      (eclReturn: EclReturn, ukRevenue: Long, calculatedLiability: CalculatedLiability) =>
        val updatedReturn = eclReturn.copy(relevantApRevenue = Some(ukRevenue))

        when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(Some(Future.successful(updatedReturn.copy(calculatedLiability = Some(calculatedLiability)))))

        await(
          pageNavigator.nextPage(CheckMode, updatedReturn)(fakeRequest)
        ) shouldBe routes.AmountDueController.onPageLoad(CheckMode)
    }

    "return a Call to the answers are invalid page in CheckMode when the ECL return data is invalid" in forAll {
      (eclReturn: EclReturn) =>
        val updatedReturn = eclReturn.copy(relevantAp12Months = None)

        when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(None)

        await(pageNavigator.nextPage(CheckMode, updatedReturn)(fakeRequest)) shouldBe routes.NotableErrorController
          .answersAreInvalid()
    }

    "return a Call to the answers are invalid page in either mode when the ECL return does not contain an answer for UK revenue" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        val updatedReturn = eclReturn.copy(relevantApRevenue = None)

        await(pageNavigator.nextPage(mode, updatedReturn)(fakeRequest)) shouldBe routes.NotableErrorController
          .answersAreInvalid()
    }

  }
}
