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
import org.mockito.ArgumentMatchers.any
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.forms.SavedResponsesFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.services.{ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.SavedResponsesView

import scala.concurrent.Future

class SavedResponsesControllerSpec extends SpecBase {

  val mockEclReturnsService: ReturnsService                  = mock[ReturnsService]
  val mockSessionService: SessionService                     = mock[SessionService]
  val view: SavedResponsesView                               = app.injector.instanceOf[SavedResponsesView]
  val savedResponsesFormProvider: SavedResponsesFormProvider = app.injector.instanceOf[SavedResponsesFormProvider]

  class TestContext(eclReturnData: EclReturn) {
    val controller = new SavedResponsesController(
      mcc,
      fakeAuthorisedAction(internalId),
      mockSessionService,
      mockEclReturnsService,
      savedResponsesFormProvider,
      view,
      fakeDataRetrievalAction(eclReturnData, Some(periodKey))
    )
  }

  override def beforeEach(): Unit = {
    reset(mockEclReturnsService)
    reset(mockSessionService)
  }

  "onPageLoad" should {
    "should return Ok with SavedResponsesFormProvider" in forAll { (eclReturn: EclReturn) =>
      new TestContext(eclReturn) {
        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK
      }
    }
  }

  "onSubmit" should {
    "should redirect to return url saved in session when the user wants to continue with their saved answers" in forAll {
      (eclReturn: EclReturn) =>
        new TestContext(eclReturn) {
          val path = "/return-url"
          when(mockSessionService.get(any(), any(), any())(any())).thenReturn(EitherT.fromEither[Future](Right(path)))

          val result: Future[Result] = controller.onSubmit()(
            fakeRequest.withFormUrlEncodedBody(("value", "true"))
          )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(path)
        }
    }

    "should redirect to the RelevantAp12MonthsController when the user wants to delete their saved answers and start again" in forAll {
      (eclReturn: EclReturn) =>
        new TestContext(eclReturn) {
          when(mockSessionService.delete(any())(any())).thenReturn(EitherT.fromEither[Future](Right(())))

          when(mockEclReturnsService.deleteReturn(any())(any())).thenReturn(EitherT.fromEither[Future](Right(())))

          val result: Future[Result] = controller.onSubmit()(
            fakeRequest.withFormUrlEncodedBody(("value", "false"))
          )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.RelevantAp12MonthsController.onPageLoad(NormalMode).url)
        }
    }
  }
}
