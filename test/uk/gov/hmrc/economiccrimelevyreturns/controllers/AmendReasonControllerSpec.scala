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
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.forms.AmendReasonFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.navigation.AmendReasonPageNavigator
import uk.gov.hmrc.economiccrimelevyreturns.views.html.AmendReasonView

import scala.concurrent.Future

class AmendReasonControllerSpec extends SpecBase {

  val view: AmendReasonView                 = app.injector.instanceOf[AmendReasonView]
  val formProvider: AmendReasonFormProvider = new AmendReasonFormProvider()
  val form: Form[String]                    = formProvider()

  val pageNavigator: AmendReasonPageNavigator = new AmendReasonPageNavigator() {
    override protected def navigateInNormalMode(eclReturn: EclReturn): Call = onwardRoute
  }

  val mockEclReturnsConnector: EclReturnsConnector = mock[EclReturnsConnector]

  class TestContext(returnsData: EclReturn) {
    val controller = new AmendReasonController(
      mcc,
      fakeAuthorisedAction(returnsData.internalId),
      fakeDataRetrievalAction(returnsData),
      mockEclReturnsConnector,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn.copy(amendReason = None)) {
        val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (eclReturn: EclReturn, reason: String) =>
        new TestContext(eclReturn.copy(amendReason = Some(reason))) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(reason), NormalMode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided amendment reason then redirect to the next page" in forAll(
      Arbitrary.arbitrary[EclReturn],
      nonEmptyString
    ) { (eclReturn: EclReturn, reason: String) =>
      new TestContext(eclReturn) {
        val updatedReturn: EclReturn = eclReturn.copy(amendReason = Some(reason))

        when(mockEclReturnsConnector.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(Future.successful(updatedReturn))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", reason)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        val result: Future[Result]       = controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[String] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, NormalMode)(fakeRequest, messages).toString
      }
    }
  }
}
