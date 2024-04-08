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
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.cleanup.UkRevenueDataCleanup
import uk.gov.hmrc.economiccrimelevyreturns.forms.UkRevenueFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, LiabilityCalculationError}
import uk.gov.hmrc.economiccrimelevyreturns.models.{Band, CalculatedLiability, CheckMode, EclReturn, FirstTimeReturn, Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclCalculatorService, ReturnsService}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.UkRevenueView
import uk.gov.hmrc.economiccrimelevyreturns.models.Band._

import scala.concurrent.Future

class UkRevenueControllerSpec extends SpecBase {

  val view: UkRevenueView                 = app.injector.instanceOf[UkRevenueView]
  val formProvider: UkRevenueFormProvider = new UkRevenueFormProvider()
  val form: Form[BigDecimal]              = formProvider()

  val mockEclReturnsService: ReturnsService         = mock[ReturnsService]
  val mockEclLiabilityService: EclCalculatorService = mock[EclCalculatorService]

  val dataCleanup: UkRevenueDataCleanup = new UkRevenueDataCleanup {
    override def cleanup(eclReturn: EclReturn): EclReturn = eclReturn
  }

  override def beforeEach(): Unit = {
    reset(mockEclReturnsService)
    reset(mockEclLiabilityService)
  }

  class TestContext(eclReturnData: EclReturn) {
    val controller = new UkRevenueController(
      mcc,
      fakeAuthorisedAction(internalId),
      fakeDataRetrievalAction(eclReturnData),
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
        new TestContext(eclReturn.copy(relevantApRevenue = None)) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, mode)(fakeRequest, messages).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (eclReturn: EclReturn, ukRevenue: Long, mode: Mode) =>
        new TestContext(
          eclReturn.copy(relevantApRevenue = Some(ukRevenue))
        ) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(ukRevenue), mode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "return a Call to the Aml regulated activity for full financial year page in NormalMode when the calculated band size is not Small" in forAll(
      arbEclReturn.arbitrary,
      arbCalculatedLiability.arbitrary,
      Gen.oneOf[Band](Medium, Large, VeryLarge),
      bigDecimalInRange(1, 1000)
    ) { (eclReturn: EclReturn, calculatedLiability: CalculatedLiability, calculatedBand: Band, ukRevenue: BigDecimal) =>
      new TestContext(eclReturn.copy(relevantApRevenue = None)) {
        val updatedReturn: EclReturn = dataCleanup.cleanup(eclReturn.copy(relevantApRevenue = Some(ukRevenue)))

        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val liability: CalculatedLiability = calculatedLiability.copy(calculatedBand = calculatedBand)
        when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(
            EitherT[Future, LiabilityCalculationError, CalculatedLiability](
              Future.successful(Right(liability))
            )
          )

        val calculatedReturn: EclReturn = updatedReturn.copy(calculatedLiability = Some(liability))
        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(calculatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", ukRevenue.toString())))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.AmlRegulatedActivityController.onPageLoad(NormalMode).url)
      }
    }

    "return a Call to the Aml regulated activity for full financial year page in CheckMode when the calculated band size is not Small and the AML activity answer has not been provided" in forAll(
      arbEclReturn.arbitrary,
      arbCalculatedLiability.arbitrary,
      Gen.oneOf[Band](Medium, Large, VeryLarge),
      bigDecimalInRange(1, 1000)
    ) { (eclReturn: EclReturn, calculatedLiability: CalculatedLiability, calculatedBand: Band, ukRevenue: BigDecimal) =>
      val eclReturnNoAmlActivity =
        eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = None, relevantAp12Months = Some(true))
      new TestContext(eclReturnNoAmlActivity.copy(relevantApRevenue = None)) {
        val updatedReturn: EclReturn = dataCleanup.cleanup(
          eclReturnNoAmlActivity.copy(
            relevantApRevenue = Some(ukRevenue)
          )
        )

        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val liability: CalculatedLiability = calculatedLiability.copy(calculatedBand = calculatedBand)
        when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(
            EitherT[Future, LiabilityCalculationError, CalculatedLiability](
              Future.successful(Right(liability))
            )
          )

        val calculatedReturn: EclReturn = updatedReturn.copy(calculatedLiability = Some(liability))
        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(calculatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val result: Future[Result] =
          controller.onSubmit(CheckMode)(fakeRequest.withFormUrlEncodedBody(("value", ukRevenue.toString)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.AmlRegulatedActivityController.onPageLoad(CheckMode).url)
      }
    }

    "return a Call to the ECL amount due page in CheckMode when the calculated band size is not Small and the AML activity answer has been provided" in forAll(
      arbEclReturn.arbitrary,
      bigDecimalInRange(1, 1000),
      arbCalculatedLiability.arbitrary,
      Gen.oneOf[Band](Medium, Large, VeryLarge)
    ) { (eclReturn: EclReturn, ukRevenue: BigDecimal, calculatedLiability: CalculatedLiability, calculatedBand: Band) =>
      val eclReturnNoAmlActivity =
        eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = Some(true), relevantAp12Months = Some(true))
      new TestContext(eclReturnNoAmlActivity.copy(relevantApRevenue = None)) {
        val updatedReturn: EclReturn = dataCleanup.cleanup(
          eclReturnNoAmlActivity.copy(
            relevantApRevenue = Some(ukRevenue)
          )
        )

        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val liability: CalculatedLiability = calculatedLiability.copy(calculatedBand = calculatedBand)
        when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(
            EitherT[Future, LiabilityCalculationError, CalculatedLiability](
              Future.successful(Right(liability))
            )
          )

        val calculatedReturn: EclReturn = updatedReturn.copy(calculatedLiability = Some(liability))
        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(calculatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val result: Future[Result] =
          controller.onSubmit(CheckMode)(fakeRequest.withFormUrlEncodedBody(("value", ukRevenue.toString)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.AmountDueController.onPageLoad(CheckMode).url)
      }
    }

    "return a Call to the ECL amount due page in either mode when the calculated band size is Small (nil return)" in forAll {
      (eclReturn: EclReturn, calculatedLiability: CalculatedLiability, mode: Mode) =>
        val ukRevenue     = bigDecimalInRange(1, 1000).sample.get
        val updatedReturn = eclReturn.copy(relevantAp12Months = Some(true))
        val liability     = calculatedLiability.copy(calculatedBand = Small)

        new TestContext(updatedReturn.copy(relevantApRevenue = None)) {
          val cleansedReturn: EclReturn = dataCleanup.cleanup(
            updatedReturn.copy(
              relevantApRevenue = Some(ukRevenue)
            )
          )

          when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(cleansedReturn))(any()))
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

          when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(cleansedReturn))(any()))
            .thenReturn(
              EitherT[Future, LiabilityCalculationError, CalculatedLiability](
                Future.successful(Right(liability))
              )
            )

          when(
            mockEclReturnsService.upsertReturn(
              ArgumentMatchers.eq(cleansedReturn.copy(calculatedLiability = Some(liability)))
            )(any())
          )
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

          val nilReturn: EclReturn =
            cleansedReturn.copy(carriedOutAmlRegulatedActivityForFullFy = None, amlRegulatedActivityLength = None)

          when(
            mockEclReturnsService.upsertReturn(
              ArgumentMatchers.eq(nilReturn)
            )(any())
          )
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

          when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(nilReturn))(any()))
            .thenReturn(
              EitherT[Future, LiabilityCalculationError, CalculatedLiability](
                Future.successful(Right(liability))
              )
            )

          val result: Future[Result] =
            controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", ukRevenue.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.AmountDueController.onPageLoad(mode).url)
        }
    }

    "return a Call to the answers are invalid page in either mode when the ECL return data is invalid" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        new TestContext(eclReturn.copy(relevantApRevenue = None)) {
          val ukRevenue: BigDecimal    = bigDecimalInRange(1, 1000).sample.get
          val updatedReturn: EclReturn = dataCleanup.cleanup(
            eclReturn.copy(
              relevantApRevenue = Some(ukRevenue)
            )
          )
          when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

          when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
            .thenReturn(
              EitherT[Future, LiabilityCalculationError, CalculatedLiability](
                Future.successful(Left(LiabilityCalculationError.BadRequest("ECL Return data is invalid")))
              )
            )

          val result: Future[Result] =
            controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", ukRevenue.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll(
      Arbitrary.arbitrary[EclReturn],
      Gen.alphaStr,
      Arbitrary.arbitrary[Mode]
    ) { (eclReturn: EclReturn, invalidRevenue: String, mode: Mode) =>
      new TestContext(eclReturn.copy(relevantApRevenue = None)) {
        val result: Future[Result]           =
          controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", invalidRevenue)))
        val formWithErrors: Form[BigDecimal] = form.bind(Map("value" -> invalidRevenue))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, mode)(fakeRequest, messages).toString
      }
    }
  }

  "return a Call to the check your answers page in CheckMode when no data gas changed" in forAll(
    arbEclReturn.arbitrary,
    bigDecimalInRange(1, 1000),
    arbCalculatedLiability.arbitrary,
    Gen.oneOf[Band](Medium, Large, VeryLarge),
    Arbitrary.arbitrary[String]
  ) {
    (
      eclReturn: EclReturn,
      ukRevenue: BigDecimal,
      calculatedLiability: CalculatedLiability,
      calculatedBand: Band,
      name: String
    ) =>
      val baseReturn = clearContact(eclReturn).copy(contactName = Some(name))
      new TestContext(baseReturn.copy(relevantApRevenue = Some(ukRevenue))) {
        val updatedReturn: EclReturn = dataCleanup.cleanup(
          baseReturn.copy(
            relevantApRevenue = Some(ukRevenue)
          )
        )

        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val liability: CalculatedLiability = calculatedLiability.copy(calculatedBand = calculatedBand)
        when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(
            EitherT[Future, LiabilityCalculationError, CalculatedLiability](
              Future.successful(Right(liability))
            )
          )

        val calculatedReturn: EclReturn = updatedReturn.copy(calculatedLiability = Some(liability))
        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(calculatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val result: Future[Result] =
          controller.onSubmit(CheckMode)(fakeRequest.withFormUrlEncodedBody(("value", ukRevenue.toString)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(
          routes.CheckYourAnswersController.onPageLoad(eclReturn.returnType.getOrElse(FirstTimeReturn)).url
        )
      }
  }

  "return a Call to the amount due page in CheckMode when no data gas changed" in forAll(
    arbEclReturn.arbitrary,
    bigDecimalInRange(1, 1000),
    arbCalculatedLiability.arbitrary,
    Gen.oneOf[Band](Medium, Large, VeryLarge)
  ) {
    (
      eclReturn: EclReturn,
      ukRevenue: BigDecimal,
      calculatedLiability: CalculatedLiability,
      calculatedBand: Band
    ) =>
      val baseReturn = clearContact(eclReturn).copy(contactName = None)
      new TestContext(baseReturn.copy(relevantApRevenue = Some(ukRevenue))) {
        val updatedReturn: EclReturn = dataCleanup.cleanup(
          baseReturn.copy(
            relevantApRevenue = Some(ukRevenue)
          )
        )

        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val liability: CalculatedLiability = calculatedLiability.copy(calculatedBand = calculatedBand)
        when(mockEclLiabilityService.calculateLiability(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(
            EitherT[Future, LiabilityCalculationError, CalculatedLiability](
              Future.successful(Right(liability))
            )
          )

        val calculatedReturn: EclReturn = updatedReturn.copy(calculatedLiability = Some(liability))
        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(calculatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val result: Future[Result] =
          controller.onSubmit(CheckMode)(fakeRequest.withFormUrlEncodedBody(("value", ukRevenue.toString)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.AmountDueController.onPageLoad(CheckMode).url)
      }
  }
}
