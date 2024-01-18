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
import org.scalacheck.Gen
import uk.gov.hmrc.economiccrimelevyreturns.ValidEclReturn
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.{EclCalculatorConnector, ReturnsConnector}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.LiabilityCalculationError
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn}
import uk.gov.hmrc.http.UpstreamErrorResponse
import play.api.http.Status.INTERNAL_SERVER_ERROR

import scala.concurrent.Future

class EclLiabilityServiceSpec extends SpecBase {
  private val mockEclCalculatorConnector = mock[EclCalculatorConnector]
  private val service                    = new EclCalculatorService(mockEclCalculatorConnector)

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

        val result = await(service.calculateLiability(validEclReturn.eclReturn)(fakeRequest).value)

        result shouldBe Right(calculatedLiability)
    }

    "return InternalUnexpectedError when the ECL return does not contain any of the required AP and/or AML answers" in forAll {
      eclReturn: EclReturn =>
        val returnWithoutAp12Months = eclReturn.copy(
          relevantAp12Months = None
        )

        val returnWithoutApLength = eclReturn.copy(
          relevantAp12Months = Some(false),
          relevantApLength = None
        )

        val returnWithoutApRevenue = eclReturn.copy(
          relevantAp12Months = Some(true),
          relevantApRevenue = None
        )

        Seq(returnWithoutApRevenue, returnWithoutAp12Months, returnWithoutApLength).map(updatedReturn =>
          await(service.calculateLiability(updatedReturn)(fakeRequest).value) shouldBe Left(
            LiabilityCalculationError.InternalUnexpectedError(None, Some("Missing expected value."))
          )
        )
    }

    "return 5xx UpstreamErrorResponse when unable to get calculated liability" in { (validEclReturn: ValidEclReturn) =>
      val errorCode = INTERNAL_SERVER_ERROR

      when(
        mockEclCalculatorConnector.calculateLiability(
          ArgumentMatchers.eq(validEclReturn.eclLiabilityCalculationData.amlRegulatedActivityLength),
          ArgumentMatchers.eq(validEclReturn.eclLiabilityCalculationData.relevantApLength),
          ArgumentMatchers.eq(validEclReturn.eclLiabilityCalculationData.relevantApRevenue)
        )(any())
      ).thenReturn(Future.failed(UpstreamErrorResponse("Internal server error", errorCode)))

      val result = await(service.calculateLiability(validEclReturn.eclReturn)(fakeRequest).value)

      result shouldBe Left(LiabilityCalculationError.BadGateway(reason = "Internal server error", code = errorCode))
    }

    "return 0 when user hasn't provided AML regulated activity length yet" in {
      val amlRegulatedActivityLength = service.calculateAmlRegulatedActivityLength(false, None)

      amlRegulatedActivityLength shouldBe 0
    }

    "return Some of full year regulated activity when user answered Yes" in {
      val amlRegulatedActivityLength = service.calculateAmlRegulatedActivityLength(true, None)

      amlRegulatedActivityLength shouldBe FullYear
    }

    "return amlRegulatedActivityLength when the user hasn't yet provided a length" in forAll(Gen.chooseNum(1, 364)) {
      days =>
        val amlRegulatedActivityLength = service.calculateAmlRegulatedActivityLength(false, Some(days))

        amlRegulatedActivityLength shouldBe days
    }
  }

}
