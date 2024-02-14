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
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.RegistrationConnector
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataHandlingError
import uk.gov.hmrc.economiccrimelevyreturns.models.GetSubscriptionResponse
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class RegistrationServiceSpec extends SpecBase {
  val mockEclRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  val mockAuditService: AuditService                      = mock[AuditService]
  val service                                             = new RegistrationService(mockEclRegistrationConnector)

  override def beforeEach(): Unit = {
    reset(mockAuditService)
    reset(mockEclRegistrationConnector)
  }

  "getRegistration" should {
    "return a registration" in forAll { (eclReference: String, subscription: GetSubscriptionResponse) =>
      beforeEach()

      when(mockEclRegistrationConnector.getSubscription(any())(any()))
        .thenReturn(Future.successful(subscription))

      val result = await(service.getSubscription(eclReference).value)

      result shouldBe Right(subscription)
    }

    "return DataHandlingError.BadGateway when when call to returns connector fails with 5xx error" in forAll {
      eclReference: String =>
        beforeEach()
        val errorCode = INTERNAL_SERVER_ERROR
        val message   = "INTERNAL_SERVER_ERROR"

        when(mockEclRegistrationConnector.getSubscription(ArgumentMatchers.eq(eclReference))(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse.apply(message, errorCode)))

        val result =
          await(service.getSubscription(eclReference).value)

        result shouldBe Left(DataHandlingError.BadGateway(message, errorCode))
    }

    "return DataHandlingError.InternalUnexpectedError when when call to returns connector fails with an unexpected error" in forAll {
      eclReference: String =>
        beforeEach()
        val throwable: Exception = new Exception()

        when(mockEclRegistrationConnector.getSubscription(ArgumentMatchers.eq(eclReference))(any()))
          .thenReturn(Future.failed(throwable))

        val result =
          await(service.getSubscription(eclReference).value)

        result shouldBe Left(DataHandlingError.InternalUnexpectedError(Some(throwable), None))
    }
  }
}
