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

package uk.gov.hmrc.economiccrimelevyreturns.testonly.controllers

import org.mockito.ArgumentMatchers.any
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.testonly.connectors.TestOnlyConnector
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class TestOnlyControllerSpec extends SpecBase {

  val mockConnector: TestOnlyConnector = mock[TestOnlyConnector]

  class TestContext(returnsData: EclReturn) {
    val controller = new TestOnlyController(
      mcc,
      fakeAuthorisedAction(returnsData.internalId),
      fakeDataRetrievalAction(returnsData),
      mockConnector
    )
  }

  val response: HttpResponse = HttpResponse(OK, "")

  "clearAllData" should {
    "return as expected" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        when(mockConnector.clearAllData()(any()))
          .thenReturn(Future.successful(response))

        val result: Future[Result] = controller.clearAllData()(fakeRequest)

        status(result)          shouldBe response.status
        contentAsString(result) shouldBe response.body
      }
    }
  }

  "clearCurrentData" should {
    "return as expected" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        when(mockConnector.clearCurrentData()(any()))
          .thenReturn(Future.successful(response))

        val result: Future[Result] = controller.clearCurrentData()(fakeRequest)

        status(result)          shouldBe response.status
        contentAsString(result) shouldBe response.body
      }
    }
  }

  "getReturnData" should {
    "return as expected" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        val result: Future[Result] = controller.getReturnData()(fakeRequest)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe Json.stringify(Json.toJson[EclReturn](eclReturn))
      }
    }
  }
}
