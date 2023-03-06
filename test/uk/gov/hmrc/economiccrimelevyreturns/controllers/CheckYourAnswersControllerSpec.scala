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

import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.FakeValidatedReturnAction
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers._
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.govuk.summarylist._
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
      new FakeValidatedReturnAction(eclReturn),
      mcc,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
          ReturnDataRequest(fakeRequest, eclReturn.internalId, eclReturn)
        implicit val messages: Messages                                           = messagesApi.preferred(returnDataRequest)

        val result: Future[Result] = controller.onPageLoad()(returnDataRequest)

        val eclDetails: SummaryList = SummaryListViewModel(
          rows = Seq(
            UkRevenueSummary.row()
          ).flatten
        ).withCssClass("govuk-!-margin-bottom-9")

        val contactDetails: SummaryList = SummaryListViewModel(
          rows = Seq(
            ContactNameSummary.row(),
            ContactRoleSummary.row(),
            ContactEmailSummary.row(),
            ContactNumberSummary.row()
          ).flatten
        ).withCssClass("govuk-!-margin-bottom-9")

        status(result)          shouldBe OK
        contentAsString(result) shouldBe view(eclDetails, contactDetails)(fakeRequest, messages).toString
      }
    }
  }

  "onSubmit" should {
    "submit the ECL return" in {
      // TODO: Add the test as part of ECL-204
      pending
    }
  }

}
