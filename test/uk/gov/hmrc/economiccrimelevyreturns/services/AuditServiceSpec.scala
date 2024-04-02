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

import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.ReturnType
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.AuditError
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.Future

class AuditServiceSpec extends ServiceSpec {

  val mockAuditConnector: AuditConnector = mock[AuditConnector]

  val service = new AuditService(
    mockAuditConnector
  )

  def getReturnTypeAsOption(isPresent: Boolean, returnType: ReturnType): Option[ReturnType] =
    isPresent match {
      case true  => Some(returnType)
      case false => None
    }

  "auditReturnStarted" should {
    "return normally if successful" in forAll {
      (internalId: String, eclReference: String, returnType: ReturnType, isNoneReturnType: Boolean) =>
        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        val result = await(
          service
            .auditReturnStarted(internalId, eclReference, getReturnTypeAsOption(isNoneReturnType, returnType))
            .value
        )

        result shouldBe Right(())
    }

    "return error if failure" in forAll {
      (internalId: String, eclReference: String, returnType: ReturnType, is5xxError: Boolean) =>
        val code = getErrorCode(is5xxError)

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.failed(testException))

        await(service.auditReturnStarted(internalId, eclReference, Some(returnType)).value) shouldBe
          Left(
            AuditError.InternalUnexpectedError(testException.getMessage(), Some(testException))
          )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse(code.toString, code)))

        await(service.auditReturnStarted(internalId, eclReference, Some(returnType)).value) shouldBe
          Left(AuditError.BadGateway(s"Audit Return Started Failed - ${code.toString}", code))
    }
  }
}
