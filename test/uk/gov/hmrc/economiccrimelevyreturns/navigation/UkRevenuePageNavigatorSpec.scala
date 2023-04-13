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
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.{Band, CalculatedLiability, CheckMode, EclReturn, Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.Band._
import uk.gov.hmrc.economiccrimelevyreturns.services.EclLiabilityService

import scala.concurrent.Future

class UkRevenuePageNavigatorSpec extends SpecBase {

  val mockEclLiabilityService: EclLiabilityService = mock[EclLiabilityService]
  val mockEclReturnsConnector: EclReturnsConnector = mock[EclReturnsConnector]

  val pageNavigator = new UkRevenuePageNavigator(mockEclLiabilityService, mockEclReturnsConnector)

  "nextPage" should {
    "return a Call to the Aml regulated activity for full financial year page in NormalMode when the calculated band size is not Small" in forAll(
      arbEclReturn.arbitrary,
      Arbitrary.arbitrary[Long],
      arbCalculatedLiability.arbitrary,
      Gen.oneOf[Band](Medium, Large, VeryLarge)
    ) { (eclReturn: EclReturn, ukRevenue: Long, calculatedLiability: CalculatedLiability, calculatedBand: Band) =>
      val updatedReturn = eclReturn.copy(relevantApRevenue = Some(ukRevenue))

      when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
        .thenReturn(
          Some(
            Future.successful(
              updatedReturn.copy(calculatedLiability = Some(calculatedLiability.copy(calculatedBand = calculatedBand)))
            )
          )
        )

      await(
        pageNavigator.nextPage(NormalMode, updatedReturn)(fakeRequest)
      ) shouldBe routes.AmlRegulatedActivityController.onPageLoad(NormalMode)
    }

    "return a Call to the Aml regulated activity for full financial year page in CheckMode when the calculated band size is not Small and the AML activity answer has not been provided" in forAll(
      arbEclReturn.arbitrary,
      Arbitrary.arbitrary[Long],
      arbCalculatedLiability.arbitrary,
      Gen.oneOf[Band](Medium, Large, VeryLarge)
    ) { (eclReturn: EclReturn, ukRevenue: Long, calculatedLiability: CalculatedLiability, calculatedBand: Band) =>
      val updatedReturn = eclReturn.copy(
        relevantApRevenue = Some(ukRevenue),
        carriedOutAmlRegulatedActivityForFullFy = None,
        amlRegulatedActivityLength = None
      )

      when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
        .thenReturn(
          Some(
            Future.successful(
              updatedReturn.copy(calculatedLiability = Some(calculatedLiability.copy(calculatedBand = calculatedBand)))
            )
          )
        )

      await(
        pageNavigator.nextPage(CheckMode, updatedReturn)(fakeRequest)
      ) shouldBe routes.AmlRegulatedActivityController.onPageLoad(CheckMode)
    }

    "return a Call to the ECL amount due page in CheckMode when the calculated band size is not Small and the AML activity answer has been provided" in forAll(
      arbEclReturn.arbitrary,
      Arbitrary.arbitrary[Long],
      arbCalculatedLiability.arbitrary,
      Gen.oneOf[Band](Medium, Large, VeryLarge)
    ) { (eclReturn: EclReturn, ukRevenue: Long, calculatedLiability: CalculatedLiability, calculatedBand: Band) =>
      val updatedReturn = eclReturn.copy(
        relevantApRevenue = Some(ukRevenue),
        carriedOutAmlRegulatedActivityForFullFy = Some(true),
        amlRegulatedActivityLength = None
      )

      when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
        .thenReturn(
          Some(
            Future.successful(
              updatedReturn.copy(calculatedLiability = Some(calculatedLiability.copy(calculatedBand = calculatedBand)))
            )
          )
        )

      await(
        pageNavigator.nextPage(CheckMode, updatedReturn)(fakeRequest)
      ) shouldBe routes.AmountDueController.onPageLoad(CheckMode)
    }

    "return a Call to the ECL amount due page in either mode when the calculated band size is Small (nil return)" in forAll {
      (eclReturn: EclReturn, ukRevenue: Long, calculatedLiability: CalculatedLiability, mode: Mode) =>
        val updatedReturn    = eclReturn.copy(relevantApRevenue = Some(ukRevenue))
        val calculatedReturn =
          updatedReturn.copy(calculatedLiability = Some(calculatedLiability.copy(calculatedBand = Small)))
        val nilReturn        =
          calculatedReturn.copy(carriedOutAmlRegulatedActivityForFullFy = None, amlRegulatedActivityLength = None)

        when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(Some(Future.successful(calculatedReturn)))

        when(mockEclReturnsConnector.upsertReturn(ArgumentMatchers.eq(nilReturn))(any()))
          .thenReturn(Future.successful(nilReturn))

        await(
          pageNavigator.nextPage(mode, updatedReturn)(fakeRequest)
        ) shouldBe routes.AmountDueController.onPageLoad(mode)
    }

    "return a Call to the answers are invalid page in either mode when the ECL return data is invalid" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        val updatedReturn = eclReturn.copy(relevantAp12Months = None)

        when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(None)

        await(pageNavigator.nextPage(mode, updatedReturn)(fakeRequest)) shouldBe routes.NotableErrorController
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
