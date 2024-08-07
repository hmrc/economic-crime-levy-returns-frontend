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
import uk.gov.hmrc.economiccrimelevyreturns.forms.RelevantApLengthFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.Band.Small
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, LiabilityCalculationError}
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclCalculatorService, ReturnsService}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.RelevantApLengthView

import scala.concurrent.Future

class RelevantApLengthControllerSpec extends SpecBase {

  val view: RelevantApLengthView                 = app.injector.instanceOf[RelevantApLengthView]
  val formProvider: RelevantApLengthFormProvider = new RelevantApLengthFormProvider()
  val form: Form[Int]                            = formProvider()

  val mockEclReturnsService: ReturnsService         = mock[ReturnsService]
  val mockEclLiabilityService: EclCalculatorService = mock[EclCalculatorService]

  override def beforeEach(): Unit = {
    reset(mockEclLiabilityService)
    reset(mockEclReturnsService)
  }

  class TestContext(eclReturnData: EclReturn) {
    val controller = new RelevantApLengthController(
      mcc,
      fakeAuthorisedAction(eclReturnData.internalId),
      fakeDataRetrievalAction(eclReturnData),
      mockEclReturnsService,
      mockEclLiabilityService,
      formProvider,
      view,
      fakeNoOpStoreUrlAction
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        new TestContext(eclReturn.copy(relevantApLength = None)) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, mode)(fakeRequest, messages).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (eclReturn: EclReturn, relevantApLength: Int, mode: Mode) =>
        new TestContext(
          eclReturn.copy(relevantApLength = Some(relevantApLength))
        ) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(relevantApLength), mode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided relevant AP length then redirect to the UK revenue page in NormalMode" in forAll(
      Arbitrary.arbitrary[EclReturn],
      Gen.chooseNum[Int](MinMaxValues.apDaysMin, MinMaxValues.apDaysMax)
    ) { (eclReturn: EclReturn, relevantApLength: Int) =>
      new TestContext(eclReturn.copy(relevantApLength = None)) {
        val updatedReturn: EclReturn = eclReturn.copy(relevantApLength = Some(relevantApLength))

        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", relevantApLength.toString)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.UkRevenueController.onPageLoad(NormalMode).url)
      }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll(
      Arbitrary.arbitrary[EclReturn],
      Gen.alphaStr,
      Arbitrary.arbitrary[Mode]
    ) { (eclReturn: EclReturn, invalidLength: String, mode: Mode) =>
      new TestContext(eclReturn.copy(relevantApLength = None)) {
        val result: Future[Result]    =
          controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", invalidLength)))
        val formWithErrors: Form[Int] = form.bind(Map("value" -> invalidLength))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, mode)(fakeRequest, messages).toString
      }
    }

    "save the provided relevant AP length then redirect to the check your answers page if no data change" in forAll(
      Arbitrary.arbitrary[EclReturn],
      Gen.chooseNum[Int](MinMaxValues.apDaysMin, MinMaxValues.apDaysMax),
      Arbitrary.arbitrary[String]
    ) { (eclReturn: EclReturn, relevantApLength: Int, name: String) =>
      val baseReturn = clearContact(eclReturn).copy(contactName = Some(name))
      new TestContext(baseReturn.copy(relevantApLength = Some(relevantApLength))) {
        val updatedReturn: EclReturn = baseReturn.copy(relevantApLength = Some(relevantApLength))

        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val result: Future[Result] =
          controller.onSubmit(CheckMode)(fakeRequest.withFormUrlEncodedBody(("value", relevantApLength.toString)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(
          routes.CheckYourAnswersController.onPageLoad().url
        )
      }
    }

    "save the provided relevant AP length then redirect to the amount due page if no data change" in forAll(
      Arbitrary.arbitrary[EclReturn],
      Gen.chooseNum[Int](MinMaxValues.apDaysMin, MinMaxValues.apDaysMax)
    ) { (eclReturn: EclReturn, relevantApLength: Int) =>
      val baseReturn = clearContact(eclReturn).copy(contactName = None)
      new TestContext(baseReturn.copy(relevantApLength = Some(relevantApLength))) {
        val updatedReturn: EclReturn = baseReturn.copy(relevantApLength = Some(relevantApLength))

        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val result: Future[Result] =
          controller.onSubmit(CheckMode)(fakeRequest.withFormUrlEncodedBody(("value", relevantApLength.toString)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.AmountDueController.onPageLoad(CheckMode).url)
      }
    }
  }

  "save the provided relevant AP length then redirect to the amount page if new data" in forAll(
    Arbitrary.arbitrary[EclReturn],
    Gen.chooseNum[Int](MinMaxValues.apDaysMin, MinMaxValues.apDaysMax),
    Arbitrary.arbitrary[String],
    Arbitrary.arbitrary[CalculatedLiability]
  ) { (eclReturn: EclReturn, relevantApLength: Int, name: String, calculatedLiability: CalculatedLiability) =>
    val baseReturn = clearContact(eclReturn).copy(contactName = Some(name))
    new TestContext(baseReturn.copy(relevantApLength = None)) {

      when(mockEclReturnsService.upsertReturn(any())(any()))
        .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

      when(mockEclLiabilityService.calculateLiability(any())(any()))
        .thenReturn(
          EitherT[Future, LiabilityCalculationError, CalculatedLiability](
            Future.successful(
              Right(
                calculatedLiability.copy(
                  calculatedBand = Small
                )
              )
            )
          )
        )

      val result: Future[Result] =
        controller.onSubmit(CheckMode)(fakeRequest.withFormUrlEncodedBody(("value", relevantApLength.toString)))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.AmountDueController.onPageLoad(CheckMode).url)
    }
  }
}
