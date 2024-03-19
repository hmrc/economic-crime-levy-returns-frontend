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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.forms.SavedResponsesFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, SessionError}
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, ObligationDetails, SessionKeys}
import uk.gov.hmrc.economiccrimelevyreturns.services.{ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.SavedResponsesView

import java.time.LocalDate
import scala.concurrent.Future

class SavedResponsesControllerSpec extends SpecBase {

  val view: SavedResponsesView                 = app.injector.instanceOf[SavedResponsesView]
  val formProvider: SavedResponsesFormProvider = new SavedResponsesFormProvider()
  val form: Form[Boolean]                      = formProvider()

  val mockEclReturnsService: ReturnsService = mock[ReturnsService]
  val mockSessionService: SessionService    = mock[SessionService]

  class TestContext(returnsData: EclReturn) {
    val controller = new SavedResponsesController(
      mcc,
      fakeAuthorisedAction(returnsData.internalId),
      mockSessionService,
      mockEclReturnsService,
      formProvider,
      view,
      fakeDataRetrievalAction(returnsData, Some(testPeriodKey))
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (eclReturn: EclReturn, obligationDetails: ObligationDetails, fromFY: LocalDate, toFY: LocalDate) =>
        new TestContext(eclReturn) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "redirect to the saved page if yes is selected" in forAll { (eclReturn: EclReturn, url: String) =>
      new TestContext(eclReturn) {
        when(
          mockSessionService.get(
            any(),
            ArgumentMatchers.eq(eclReturn.internalId),
            ArgumentMatchers.eq(SessionKeys.UrlToReturnTo)
          )(any())
        )
          .thenReturn(EitherT[Future, SessionError, String](Future.successful(Right(url))))

        val result: Future[Result] =
          controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "true")))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(url)
      }
    }

    "delete the saved return and redirect to the start page if no is selected" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        when(
          mockEclReturnsService.deleteReturn(
            ArgumentMatchers.eq(eclReturn.internalId)
          )(any())
        )
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right())))

        when(
          mockSessionService.delete(
            ArgumentMatchers.eq(eclReturn.internalId)
          )(any())
        )
          .thenReturn(EitherT[Future, SessionError, Unit](Future.successful(Right())))

        val result: Future[Result] =
          controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "false")))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.StartController.onPageLoad(testPeriodKey).url)
      }
    }

    "return a Bad Request with form errors if nothing selected" in forAll { eclReturn: EclReturn =>
      new TestContext(eclReturn) {
        val tuple: (String, String)       = ("value", "")
        val formWithErrors: Form[Boolean] = form.bind(Map(tuple._1 -> tuple._2))

        val result: Future[Result] =
          controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(tuple))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors)(fakeRequest, messages).toString
      }
    }
  }
}
