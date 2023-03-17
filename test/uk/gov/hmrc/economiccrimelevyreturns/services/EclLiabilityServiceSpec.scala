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

package uk.gov.hmrc.economiccrimelevyreturns.services

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyreturns.ValidEclReturn
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.{EclCalculatorConnector, EclReturnsConnector}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn}

import scala.concurrent.Future

class EclLiabilityServiceSpec extends SpecBase {
  private val mockEclReturnsConnector    = mock[EclReturnsConnector]
  private val mockEclCalculatorConnector = mock[EclCalculatorConnector]
  private val service                    = new EclLiabilityService(mockEclReturnsConnector, mockEclCalculatorConnector)

  "calculateLiability" should {
    "return an updated ECL return containing the calculated liability" in forAll {
      (validEclReturn: ValidEclReturn, calculatedLiability: CalculatedLiability) =>
        val updatedReturn = validEclReturn.eclReturn.copy(calculatedLiability = Some(calculatedLiability))

        when(
          mockEclCalculatorConnector.calculateLiability(
            ArgumentMatchers.eq(validEclReturn.eclLiabilityCalculationData.amlRegulatedActivityLength),
            ArgumentMatchers.eq(validEclReturn.eclLiabilityCalculationData.relevantApLength),
            ArgumentMatchers.eq(validEclReturn.eclLiabilityCalculationData.relevantApRevenue)
          )(any())
        ).thenReturn(Future.successful(calculatedLiability))

        when(mockEclReturnsConnector.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(Future.successful(updatedReturn))

        val result = await(service.calculateLiability(validEclReturn.eclReturn)(fakeRequest).value)

        result shouldBe updatedReturn
    }

    "return None when the ECL return does not contain any of the required AP and/or AML answers" in forAll {
      eclReturn: EclReturn =>
        val updatedReturn = eclReturn.copy(
          relevantAp12Months = None,
          relevantApLength = None,
          relevantApRevenue = None,
          carriedOutAmlRegulatedActivityForFullFy = None,
          amlRegulatedActivityLength = None
        )

        val result = service.calculateLiability(updatedReturn)(fakeRequest)

        result shouldBe None
    }
  }

}
