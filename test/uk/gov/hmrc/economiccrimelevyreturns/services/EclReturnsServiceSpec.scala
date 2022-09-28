/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector

import scala.concurrent.Future

class EclReturnsServiceSpec extends SpecBase {
  val mockEclReturnsConnector: EclReturnsConnector = mock[EclReturnsConnector]
  val service                                      = new EclReturnsService(mockEclReturnsConnector)

  "getOrCreateReturn" should {
    "return a created ecl return when one does not exist" in {
      when(mockEclReturnsConnector.getReturn(any())(any()))
        .thenReturn(Future.successful(None))

      when(mockEclReturnsConnector.upsertReturn(any())(any()))
        .thenReturn(Future.successful(emptyReturn))

      val result = await(service.getOrCreateReturn(internalId))
      result shouldBe emptyReturn
    }

    "return an existing ecl return" in {
      when(mockEclReturnsConnector.getReturn(any())(any()))
        .thenReturn(Future.successful(Some(emptyReturn)))

      val result = await(service.getOrCreateReturn(internalId))
      result shouldBe emptyReturn
    }
  }
}
