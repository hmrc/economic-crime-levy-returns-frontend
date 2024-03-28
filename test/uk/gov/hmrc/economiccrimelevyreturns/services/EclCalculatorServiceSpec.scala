/*
 * Copyright 2024 HM Revenue & Customs
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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclCalculatorConnector
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.LiabilityCalculationError
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class EclCalculatorServiceSpec extends ServiceSpec {

  val mockEclCalculatorConnector: EclCalculatorConnector = mock[EclCalculatorConnector]
  val service                                            = new EclCalculatorService(
    mockEclCalculatorConnector
  )

  def getValidReturn(eclReturn: EclReturn): EclReturn =
    eclReturn.copy(
      relevantAp12Months = Some(random[Boolean]),
      relevantApLength = Some(random[Int]),
      relevantApRevenue = Some(random[BigDecimal]),
      carriedOutAmlRegulatedActivityForFullFy = Some(random[Boolean]),
      amlRegulatedActivityLength = Some(random[Int])
    )

  "calculateLiability" should {
    "returns normally if success" in forAll { (eclReturn: EclReturn, calculatedLiability: CalculatedLiability) =>
      when(mockEclCalculatorConnector.calculateLiability(any(), any(), any())(any()))
        .thenReturn(Future.successful(calculatedLiability))

      val result = await(service.calculateLiability(getValidReturn(eclReturn))(fakeRequest).value)
      result shouldBe Right(calculatedLiability)
    }

    "return error if failure" in forAll { (eclReturn: EclReturn, is5xxError: Boolean) =>
      val code = getErrorCode(is5xxError)

      when(mockEclCalculatorConnector.calculateLiability(any(), any(), any())(any()))
        .thenReturn(Future.failed(testException))

      await(service.calculateLiability(getValidReturn(eclReturn))(fakeRequest).value) shouldBe
        Left(LiabilityCalculationError.InternalUnexpectedError(Some(testException)))

      when(mockEclCalculatorConnector.calculateLiability(any(), any(), any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(code.toString, code)))

      await(service.calculateLiability(getValidReturn(eclReturn))(fakeRequest).value) shouldBe
        Left(LiabilityCalculationError.BadGateway(s"Get Calculated Liability Failed - ${code.toString}", code))
    }
  }
}
