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
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.forms.AmlRegulatedActivityLengthFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, LiabilityCalculationError}
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn, Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclCalculatorService, ReturnsService}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.AmlRegulatedActivityLengthView

import scala.concurrent.Future

class AmlRegulatedActivityLengthControllerSpec extends SpecBase {

  val view: AmlRegulatedActivityLengthView                 = app.injector.instanceOf[AmlRegulatedActivityLengthView]
  val formProvider: AmlRegulatedActivityLengthFormProvider = new AmlRegulatedActivityLengthFormProvider()
  val form: Form[Int]                                      = formProvider()

  val mockEclReturnsService: ReturnsService         = mock[ReturnsService]
  val mockEclLiabilityService: EclCalculatorService = mock[EclCalculatorService]

  class TestContext(eclReturnData: EclReturn) {
    val controller = new AmlRegulatedActivityLengthController(
      mcc,
      fakeAuthorisedAction(eclReturnData.internalId),
      fakeDataRetrievalAction(eclReturnData),
      formProvider,
      view,
      mockEclLiabilityService,
      mockEclReturnsService
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        new TestContext(eclReturn.copy(amlRegulatedActivityLength = None)) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, mode)(fakeRequest, messages).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (eclReturn: EclReturn, amlRegulatedActivityLength: Int, mode: Mode) =>
        new TestContext(
          eclReturn.copy(amlRegulatedActivityLength = Some(amlRegulatedActivityLength))
        ) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(amlRegulatedActivityLength), mode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided AML regulated activity length then redirect o the UK revenue page when in NormalMode" in forAll(
      Arbitrary.arbitrary[EclReturn],
      Gen.chooseNum[Int](MinMaxValues.AmlDaysMin, MinMaxValues.AmlDaysMax),
      Arbitrary.arbitrary[CalculatedLiability],
      Arbitrary.arbitrary[Int]
    ) {
      (
        randomEclReturn: EclReturn,
        amlRegulatedActivityLength: Int,
        calculatedLiability: CalculatedLiability,
        length: Int
      ) =>
        val eclReturn = randomEclReturn.copy(
          relevantApRevenue = Some(length),
          relevantAp12Months = Some(true)
        )

        new TestContext(eclReturn) {
          val updatedReturn: EclReturn =
            eclReturn.copy(
              amlRegulatedActivityLength = Some(amlRegulatedActivityLength)
            )

          when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
            .thenReturn(
              EitherT[Future, LiabilityCalculationError, CalculatedLiability](
                Future.successful(Right(calculatedLiability))
              )
            )

          when(
            mockEclReturnsService.upsertReturn(
              ArgumentMatchers.eq(updatedReturn.copy(calculatedLiability = Some(calculatedLiability)))
            )(any())
          )
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(
              fakeRequest.withFormUrlEncodedBody(("value", amlRegulatedActivityLength.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.AmountDueController.onPageLoad(NormalMode).url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll(
      Arbitrary.arbitrary[EclReturn],
      Gen.alphaStr,
      Arbitrary.arbitrary[Mode]
    ) { (eclReturn: EclReturn, invalidLength: String, mode: Mode) =>
      new TestContext(eclReturn) {
        val result: Future[Result]    =
          controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", invalidLength)))
        val formWithErrors: Form[Int] = form.bind(Map("value" -> invalidLength))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, mode)(fakeRequest, messages).toString
      }
    }
  }
}
