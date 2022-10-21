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

package uk.gov.hmrc.economiccrimelevyreturns.controllers.actions

import org.mockito.ArgumentMatchers.any
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.{AuthorisedRequest, ReturnDataRequest}
import uk.gov.hmrc.economiccrimelevyreturns.services.EclReturnsService

import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase {

  val mockEclReturnService: EclReturnsService = mock[EclReturnsService]

  class TestDataRetrievalAction extends ReturnDataRetrievalAction(mockEclReturnService) {
    override def transform[A](request: AuthorisedRequest[A]): Future[ReturnDataRequest[A]] =
      super.transform(request)
  }

  val dataRetrievalAction =
    new TestDataRetrievalAction

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  "transform" should {
    "transform an AuthorisedRequest into a ReturnDataRequest" in forAll {
      (internalId: String, eclReferenceNumber: String) =>
        when(mockEclReturnService.getOrCreateReturn(any())(any())).thenReturn(Future(emptyReturn))

        val result: Future[ReturnDataRequest[AnyContentAsEmpty.type]] =
          dataRetrievalAction.transform(AuthorisedRequest(fakeRequest, internalId, eclReferenceNumber))

        await(result) shouldBe ReturnDataRequest(fakeRequest, internalId, emptyReturn)
    }
  }

}
