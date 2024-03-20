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

import cats.data.EitherT
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.ReturnType
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.audit.ReturnStartedEvent
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.AuditError

import scala.concurrent.Future

class AuditServiceSpec extends SpecBase {

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val testException                      = new Exception("Error")

  val service = new AuditService(
    mockAuditConnector
  )

  "auditReturnStarted" should {
    "return normally if successful" in forAll { (internalId: String, eclReference: String, returnType: ReturnType) =>
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val result = await(service.auditReturnStarted(internalId, eclReference, Some(returnType)).value)

      result shouldBe Right(())
    }

    "return an error if failure" in forAll { (internalId: String, eclReference: String, returnType: ReturnType) =>
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
        .thenReturn(Future.failed(testException))

      val result = await(service.auditReturnStarted(internalId, eclReference, Some(returnType)).value)

      result shouldBe Left(AuditError.InternalUnexpectedError(testException.getMessage, Some(testException)))
    }
  }
}
