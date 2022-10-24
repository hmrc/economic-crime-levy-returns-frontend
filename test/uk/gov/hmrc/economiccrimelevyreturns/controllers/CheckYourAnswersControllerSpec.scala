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

package uk.gov.hmrc.economiccrimelevyreturns.controllers

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.views.html.CheckYourAnswersView
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  val view: CheckYourAnswersView = app.injector.instanceOf[CheckYourAnswersView]

  class TestContext(eclReturn: EclReturn) {
    val controller = new CheckYourAnswersController(
      messagesApi,
      fakeAuthorisedAction,
      fakeDataRetrievalAction(eclReturn),
      mcc,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        val list: SummaryList = SummaryList(Seq.empty)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe view(list)(fakeRequest, messages).toString
      }
    }
  }

}
