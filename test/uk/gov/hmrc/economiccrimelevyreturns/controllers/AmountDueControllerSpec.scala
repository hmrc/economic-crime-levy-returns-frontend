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

import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers._
import uk.gov.hmrc.economiccrimelevyreturns.views.html.AmountDueView
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.govuk.summarylist._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList

import scala.concurrent.Future

class AmountDueControllerSpec extends SpecBase {

  val view: AmountDueView = app.injector.instanceOf[AmountDueView]

  val mockEclReturnsConnector: EclReturnsConnector = mock[EclReturnsConnector]

  class TestContext(eclReturnData: EclReturn) {
    val controller = new AmountDueController(
      mcc,
      fakeAuthorisedAction,
      fakeDataRetrievalAction(eclReturnData),
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when the ECL return data is valid" in forAll {
      (eclReturn: EclReturn, calculatedLiability: CalculatedLiability) =>
        val updatedReturn = eclReturn.copy(calculatedLiability = Some(calculatedLiability))

        new TestContext(updatedReturn) {
          implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
            ReturnDataRequest(fakeRequest, updatedReturn.internalId, updatedReturn)
          implicit val messages: Messages                                           = messagesApi.preferred(returnDataRequest)

          val result: Future[Result] = controller.onPageLoad()(returnDataRequest)

          val accountingDetails: SummaryList = SummaryListViewModel(
            rows = Seq(
              RelevantAp12MonthsSummary.row(),
              RelevantApLengthSummary.row(),
              UkRevenueSummary.row(),
              AmlRegulatedActivitySummary.row(),
              AmlRegulatedActivityLengthSummary.row()
            ).flatten
          ).withCssClass("govuk-!-margin-bottom-9")

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(calculatedLiability, accountingDetails)(
            fakeRequest,
            messages
          ).toString
        }
    }

    "redirect to the answers are invalid page when the ECL return data is invalid" in forAll { (eclReturn: EclReturn) =>
      val updatedReturn = eclReturn.copy(calculatedLiability = None)

      new TestContext(updatedReturn) {
        implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
          ReturnDataRequest(fakeRequest, updatedReturn.internalId, updatedReturn)

        val result: Future[Result] = controller.onPageLoad()(returnDataRequest)

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
      }
    }
  }

  "onSubmit" should {
    "redirect to the who is completing this return page" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        val result: Future[Result] = controller.onSubmit()(fakeRequest)

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.ContactNameController.onPageLoad(NormalMode).url)
      }
    }
  }
}