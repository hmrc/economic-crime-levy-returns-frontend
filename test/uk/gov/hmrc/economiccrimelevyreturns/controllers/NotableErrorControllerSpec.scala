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

package uk.gov.hmrc.economiccrimelevyreturns.controllers

import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AgentCannotSubmitReturnView, AnswersAreInvalidView, ECLReturnSubmittedAlreadyView, ReturnAmendmentAlreadyRequestedView}

import scala.concurrent.Future

class NotableErrorControllerSpec extends SpecBase {

  val answersAreInvalidView: AnswersAreInvalidView                             = app.injector.instanceOf[AnswersAreInvalidView]
  val agentCannotSubmitReturnView: AgentCannotSubmitReturnView                 = app.injector.instanceOf[AgentCannotSubmitReturnView]
  val eclReturnSubmittedAlreadyView: ECLReturnSubmittedAlreadyView             =
    app.injector.instanceOf[ECLReturnSubmittedAlreadyView]
  val returnAmendmentAlreadyRequestedView: ReturnAmendmentAlreadyRequestedView =
    app.injector.instanceOf[ReturnAmendmentAlreadyRequestedView]

  class TestContext(eclReturnData: EclReturn) {
    val controller = new NotableErrorController(
      mcc,
      fakeAuthorisedAction(eclReturnData.internalId),
      answersAreInvalidView,
      agentCannotSubmitReturnView,
      eclReturnSubmittedAlreadyView,
      returnAmendmentAlreadyRequestedView,
      appConfig
    )
  }

  "answerAreInvalid" should {
    "return OK and the correct view" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        val result: Future[Result] = controller.answersAreInvalid()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe answersAreInvalidView()(fakeRequest, messages).toString
      }
    }
  }

  "notRegistered" should {
    "return SEE_OTHER" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        val result: Future[Result] = controller.notRegistered()(fakeRequest)

        status(result) shouldBe SEE_OTHER
      }
    }
  }

  "agentCannotSubmitReturn" should {
    "return OK and the correct view" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        val result: Future[Result] = controller.agentCannotSubmitReturn()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe agentCannotSubmitReturnView()(fakeRequest, messages).toString
      }
    }
  }

  "eclReturnAlreadySubmitted" should {
    "return OK and the correct view" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        val result: Future[Result] = controller.eclReturnAlreadySubmitted()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe eclReturnSubmittedAlreadyView()(fakeRequest, messages).toString
      }
    }
  }

  "returnAmendmentAlreadyRequested" should {
    "return OK and the correct view" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        val result: Future[Result] = controller.returnAmendmentAlreadyRequested()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe returnAmendmentAlreadyRequestedView()(fakeRequest, messages).toString
      }
    }
  }
}
