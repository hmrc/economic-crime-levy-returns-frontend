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
import org.scalacheck.{Arbitrary, Gen}
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.cleanup.RelevantAp12MonthsDataCleanup
import uk.gov.hmrc.economiccrimelevyreturns.forms.RelevantAp12MonthsFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.Band.Small
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, LiabilityCalculationError}
import uk.gov.hmrc.economiccrimelevyreturns.models.{Band, CalculatedLiability, CheckMode, EclReturn, Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclCalculatorService, ReturnsService}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.RelevantAp12MonthsView

import scala.concurrent.Future

class RelevantAp12MonthsControllerSpec extends SpecBase {

  val view: RelevantAp12MonthsView                 = app.injector.instanceOf[RelevantAp12MonthsView]
  val formProvider: RelevantAp12MonthsFormProvider = new RelevantAp12MonthsFormProvider()
  val form: Form[Boolean]                          = formProvider()

  val mockEclReturnsService: ReturnsService         = mock[ReturnsService]
  val mockEclLiabilityService: EclCalculatorService = mock[EclCalculatorService]

  val dataCleanup: RelevantAp12MonthsDataCleanup = new RelevantAp12MonthsDataCleanup {
    override def cleanup(eclReturn: EclReturn): EclReturn = eclReturn
  }

  class TestContext(returnData: EclReturn) {
    val controller = new RelevantAp12MonthsController(
      mcc,
      fakeAuthorisedAction(returnData.internalId),
      fakeDataRetrievalAction(returnData),
      mockEclReturnsService,
      mockEclLiabilityService,
      formProvider,
      dataCleanup,
      view,
      fakeNoOpStoreUrlAction
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        new TestContext(eclReturn.copy(relevantAp12Months = None)) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, mode)(fakeRequest, messages).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (eclReturn: EclReturn, relevantAp12Months: Boolean, mode: Mode) =>
        new TestContext(
          eclReturn.copy(relevantAp12Months = Some(relevantAp12Months))
        ) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(form.fill(relevantAp12Months), mode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the UK Revenue page in Normal mode and relevantAp12Months is set to true" in forAll {
      (eclReturn: EclReturn) =>
        val relevantAp12Months = true
        new TestContext(eclReturn.copy(relevantAp12Months = None)) {
          val updatedReturn: EclReturn =
            dataCleanup.cleanup(eclReturn.copy(relevantAp12Months = Some(relevantAp12Months)))

          when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(
              fakeRequest.withFormUrlEncodedBody(("value", relevantAp12Months.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.UkRevenueController.onPageLoad(NormalMode).url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        new TestContext(eclReturn.copy(relevantAp12Months = None)) {
          val result: Future[Result]        = controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(formWithErrors, mode)(fakeRequest, messages).toString
        }
    }

    "save the selected answer then redirect to the check your answer page if no data change" in forAll {
      (eclReturn: EclReturn, relevantAp12Months: Boolean, length: Int, name: String) =>
        val baseReturn = clearContact(eclReturn).copy(contactName = Some(name))
        new TestContext(
          baseReturn.copy(relevantAp12Months = Some(relevantAp12Months), relevantApLength = Some(length))
        ) {
          val updatedReturn: EclReturn =
            dataCleanup.cleanup(
              baseReturn.copy(
                relevantAp12Months = Some(relevantAp12Months),
                relevantApLength = Some(length)
              )
            )

          when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(CheckMode)(
              fakeRequest.withFormUrlEncodedBody(("value", relevantAp12Months.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
        }
    }

    "save the selected answer then redirect to the amount due page if no data change" in forAll {
      (eclReturn: EclReturn, relevantAp12Months: Boolean, length: Int) =>
        val baseReturn = clearContact(eclReturn).copy(contactName = None)
        new TestContext(
          baseReturn.copy(relevantAp12Months = Some(relevantAp12Months), relevantApLength = Some(length))
        ) {
          val updatedReturn: EclReturn =
            dataCleanup.cleanup(
              baseReturn.copy(
                relevantAp12Months = Some(relevantAp12Months),
                relevantApLength = Some(length)
              )
            )

          when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(CheckMode)(
              fakeRequest.withFormUrlEncodedBody(("value", relevantAp12Months.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.AmountDueController.onPageLoad(CheckMode).url)
        }
    }

    "save the provided answer abd recalculate liability then redirect to the amount due page" in forAll {
      (
        randomEclReturn: EclReturn,
        relevantAp12Months: Boolean,
        calculatedLiability: CalculatedLiability,
        length: Int,
        band: Band,
        amlActivity: Boolean
      ) =>
        val eclReturn = clearContact(randomEclReturn).copy(
          relevantAp12Months = Some(!relevantAp12Months),
          relevantApLength = Some(length),
          carriedOutAmlRegulatedActivityForFullFy = Some(amlActivity)
        )

        new TestContext(eclReturn) {
          when(mockEclLiabilityService.calculateLiability(any())(any()))
            .thenReturn(
              EitherT[Future, LiabilityCalculationError, CalculatedLiability](
                Future.successful(Right(calculatedLiability.copy(calculatedBand = band)))
              )
            )

          when(
            mockEclReturnsService.upsertReturn(
              any()
            )(any())
          )
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(CheckMode)(
              fakeRequest.withFormUrlEncodedBody(("value", relevantAp12Months.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some((relevantAp12Months match {
            case true  => routes.AmountDueController.onPageLoad(CheckMode)
            case false => routes.RelevantApLengthController.onPageLoad(CheckMode)
          }).url)
        }
    }
  }
}
