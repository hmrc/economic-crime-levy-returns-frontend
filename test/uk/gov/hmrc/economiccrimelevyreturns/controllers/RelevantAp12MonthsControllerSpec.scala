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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.forms.RelevantAp12MonthsFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.navigation.RelevantAp12MonthsPageNavigator
import uk.gov.hmrc.economiccrimelevyreturns.views.html.RelevantAp12MonthsView

import scala.concurrent.Future

class RelevantAp12MonthsControllerSpec extends SpecBase {

  val view: RelevantAp12MonthsView                 = app.injector.instanceOf[RelevantAp12MonthsView]
  val formProvider: RelevantAp12MonthsFormProvider = new RelevantAp12MonthsFormProvider()
  val form: Form[Boolean]                          = formProvider()

  val mockEclReturnsConnector: EclReturnsConnector = mock[EclReturnsConnector]

  val pageNavigator: RelevantAp12MonthsPageNavigator = new RelevantAp12MonthsPageNavigator {
    override protected def navigateInNormalMode(eclReturn: EclReturn): Call = onwardRoute
  }

  class TestContext(returnData: EclReturn) {
    val controller = new RelevantAp12MonthsController(
      mcc,
      fakeAuthorisedAction,
      fakeDataRetrievalAction(returnData),
      mockEclReturnsConnector,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn.copy(relevantAp12Months = None)) {
        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (eclReturn: EclReturn, relevantAp12Months: Boolean) =>
        new TestContext(
          eclReturn.copy(relevantAp12Months = Some(relevantAp12Months))
        ) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(form.fill(relevantAp12Months))(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the next page" in forAll {
      (eclReturn: EclReturn, relevantAp12Months: Boolean) =>
        new TestContext(eclReturn) {
          val updatedReturn: EclReturn =
            eclReturn.copy(relevantAp12Months = Some(relevantAp12Months))

          when(mockEclReturnsConnector.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
            .thenReturn(Future.successful(updatedReturn))

          val result: Future[Result] =
            controller.onSubmit()(
              fakeRequest.withFormUrlEncodedBody(("value", relevantAp12Months.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        val result: Future[Result]        = controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors)(fakeRequest, messages).toString
      }
    }
  }
}
