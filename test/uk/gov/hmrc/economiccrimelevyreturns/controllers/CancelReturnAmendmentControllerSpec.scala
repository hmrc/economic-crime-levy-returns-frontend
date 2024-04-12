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

import cats.data.EitherT
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.forms.CancelReturnAmendmentFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.services.ReturnsService
import uk.gov.hmrc.economiccrimelevyreturns.views.html.CancelReturnAmendmentView

import scala.concurrent.Future

class CancelReturnAmendmentControllerSpec extends SpecBase {

  val view: CancelReturnAmendmentView                 = app.injector.instanceOf[CancelReturnAmendmentView]
  val formProvider: CancelReturnAmendmentFormProvider = new CancelReturnAmendmentFormProvider()
  val form: Form[Boolean]                             = formProvider()

  def getExpectedValues(cancelReturnAmendment: Boolean, eclReturn: EclReturn): (String, Int) =
    if (cancelReturnAmendment) {
      (appConfig.eclAccountUrl, 1)
    } else {
      (routes.CheckYourAnswersController.onPageLoad().url, 0)
    }

  val mockEclReturnsService: ReturnsService = mock[ReturnsService]

  class TestContext(returnData: EclReturn) {
    val controller = new CancelReturnAmendmentController(
      mcc,
      fakeAuthorisedAction(returnData.internalId),
      fakeDataRetrievalAction(returnData),
      mockEclReturnsService,
      formProvider,
      appConfig,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { (eclReturn: EclReturn) =>
      new TestContext(eclReturn.copy(relevantAp12Months = None)) {
        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form)(fakeRequest, messages).toString
      }
    }
  }

  "onSubmit" should {
    "redirect to the next page and delete data if answer is 'Yes'" in forAll {
      (eclReturn: EclReturn, cancelReturnAmendment: Boolean) =>
        new TestContext(eclReturn) {
          when(mockEclReturnsService.getOrCreateReturn(anyString(), any())(any(), any()))
            .thenReturn(EitherT.fromEither[Future](Right(eclReturn)))

          when(mockEclReturnsService.deleteReturn(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(())))

          val expected: (String, Int) = getExpectedValues(cancelReturnAmendment, eclReturn)

          val result: Future[Result] =
            controller.onSubmit()(
              fakeRequest.withFormUrlEncodedBody(("value", cancelReturnAmendment.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(expected._1)

          verify(mockEclReturnsService, times(expected._2)).deleteReturn(anyString())(any())

          reset(mockEclReturnsService)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { (eclReturn: EclReturn) =>
      new TestContext(eclReturn) {
        val result: Future[Result]        = controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors)(fakeRequest, messages).toString
      }
    }
  }
}
