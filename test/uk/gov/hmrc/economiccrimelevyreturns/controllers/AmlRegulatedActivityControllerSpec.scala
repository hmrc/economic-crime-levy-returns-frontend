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
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.cleanup.AmlRegulatedActivityDataCleanup
import uk.gov.hmrc.economiccrimelevyreturns.forms.AmlRegulatedActivityFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, LiabilityCalculationError}
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn, Mode}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclCalculatorService, ReturnsService}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.AmlRegulatedActivityView

import scala.concurrent.Future

class AmlRegulatedActivityControllerSpec extends SpecBase {

  val view: AmlRegulatedActivityView                 = app.injector.instanceOf[AmlRegulatedActivityView]
  val formProvider: AmlRegulatedActivityFormProvider = new AmlRegulatedActivityFormProvider()
  val form: Form[Boolean]                            = formProvider()

  val mockEclReturnsService: ReturnsService         = mock[ReturnsService]
  val mockEclLiabilityService: EclCalculatorService = mock[EclCalculatorService]

  val dataCleanup: AmlRegulatedActivityDataCleanup = new AmlRegulatedActivityDataCleanup {
    override def cleanup(eclReturn: EclReturn): EclReturn = eclReturn
  }

  class TestContext(eclReturnData: EclReturn) {
    val controller = new AmlRegulatedActivityController(
      mcc,
      fakeAuthorisedAction(eclReturnData.internalId),
      fakeDataRetrievalAction(eclReturnData),
      mockEclReturnsService,
      mockEclLiabilityService,
      formProvider,
      dataCleanup,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        new TestContext(eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = None)) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, mode)(fakeRequest, messages).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (eclReturn: EclReturn, carriedOutAmlRegulatedActivity: Boolean, mode: Mode) =>
        new TestContext(
          eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = Some(carriedOutAmlRegulatedActivity))
        ) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(form.fill(carriedOutAmlRegulatedActivity), mode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the Amount Due page when the user answers yes" in forAll {
      (
        eclReturn: EclReturn,
        mode: Mode,
        calculatedLiability: CalculatedLiability
      ) =>
        new TestContext(eclReturn) {
          val carriedOutAmlRegulatedActivityForFullFy = true
          val updatedReturn: EclReturn                =
            dataCleanup.cleanup(
              eclReturn.copy(
                carriedOutAmlRegulatedActivityForFullFy = Some(carriedOutAmlRegulatedActivityForFullFy)
              )
            )

          when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

          when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
            .thenReturn(
              EitherT[Future, LiabilityCalculationError, CalculatedLiability](
                Future.successful(Right(calculatedLiability))
              )
            )

          val calculatedReturn = updatedReturn.copy(calculatedLiability = Some(calculatedLiability))

          when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(calculatedReturn))(any()))
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(mode)(
              fakeRequest.withFormUrlEncodedBody(("value", carriedOutAmlRegulatedActivityForFullFy.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.AmountDueController.onPageLoad(mode).url)
        }
    }

    "save the selected answer then redirect to the AML Regulated activity length page when the user answers no" in forAll {
      (
        eclReturn: EclReturn,
        mode: Mode,
        calculatedLiability: CalculatedLiability
      ) =>
        new TestContext(eclReturn) {
          val carriedOutAmlRegulatedActivityForFullFy = false
          val updatedReturn: EclReturn                =
            dataCleanup.cleanup(
              eclReturn.copy(
                carriedOutAmlRegulatedActivityForFullFy = Some(carriedOutAmlRegulatedActivityForFullFy)
              )
            )

          when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(mode)(
              fakeRequest.withFormUrlEncodedBody(("value", carriedOutAmlRegulatedActivityForFullFy.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.AmlRegulatedActivityLengthController.onPageLoad(mode).url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        new TestContext(eclReturn) {
          val result: Future[Result]        = controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(formWithErrors, mode)(fakeRequest, messages).toString
        }
    }
  }
}
