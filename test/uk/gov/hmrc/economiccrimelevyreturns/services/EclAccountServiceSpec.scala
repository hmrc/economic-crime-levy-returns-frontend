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

import cats.data.OptionT
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclAccountConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.{ObligationData, ReturnType}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{AuditError, EclAccountError}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class EclAccountServiceSpec extends ServiceSpec {

  val mockEclAccountConnector: EclAccountConnector = mock[EclAccountConnector]
  val service                                      = new EclAccountService(
    mockEclAccountConnector
  )

  "retrieveObligationData" should {
    "return normally if success" in forAll { obligationData: ObligationData =>
      when(mockEclAccountConnector.getObligations()(any()))
        .thenReturn(OptionT[Future, ObligationData](Future.successful(Some(obligationData))))

      val result = await(service.retrieveObligationData.value)

      result shouldBe Right(Some(obligationData))
    }

    "return error if failure" in forAll { is5xxError: Boolean =>
      val code = getErrorCode(is5xxError)

      when(mockEclAccountConnector.getObligations()(any()))
        .thenReturn(OptionT[Future, ObligationData](Future.failed(testException)))

      await(service.retrieveObligationData.value) shouldBe
        Left(EclAccountError.InternalUnexpectedError(Some(testException), Some(testException.getMessage)))

      when(mockEclAccountConnector.getObligations()(any()))
        .thenReturn(OptionT[Future, ObligationData](Future.failed(UpstreamErrorResponse(code.toString, code))))

      await(service.retrieveObligationData.value) shouldBe
        Left(EclAccountError.BadGateway(code.toString, code))
    }
  }

}
