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
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataHandlingError
import uk.gov.hmrc.economiccrimelevyreturns.services.{EnrolmentStoreProxyService, ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AlreadySubmittedReturnView, ChooseReturnPeriodView, NoObligationForPeriodView, StartView}

import java.time.LocalDate
import scala.concurrent.Future

class StartControllerSpec extends SpecBase {

  val mockEnrolmentStoreProxyService: EnrolmentStoreProxyService = mock[EnrolmentStoreProxyService]
  val mockEclReturnsService: ReturnsService                      = mock[ReturnsService]
  val mockSessionService: SessionService                         = mock[SessionService]

  val view: StartView                                        = app.injector.instanceOf[StartView]
  val alreadySubmittedReturnView: AlreadySubmittedReturnView = app.injector.instanceOf[AlreadySubmittedReturnView]
  val noObligationForPeriodView: NoObligationForPeriodView   = app.injector.instanceOf[NoObligationForPeriodView]
  val chooseReturnPeriodView: ChooseReturnPeriodView         = app.injector.instanceOf[ChooseReturnPeriodView]

  class TestContext(eclReturnData: EclReturn, periodKey: String) {
    val controller = new StartController(
      mcc,
      fakeAuthorisedAction(internalId),
      fakeDataRetrievalAction(eclReturnData, Some(periodKey)),
      mockEnrolmentStoreProxyService,
      mockEclReturnsService,
      alreadySubmittedReturnView,
      noObligationForPeriodView,
      chooseReturnPeriodView,
      view,
      mockSessionService
    )
  }

  override def beforeEach(): Unit = {
    reset(mockEnrolmentStoreProxyService)
    reset(mockEclReturnsService)
    reset(mockSessionService)
  }

  "start" should {
    "redirect to the start page if the return data contains obligation details" in forAll {
      (internalId: String, obligationDetails: ObligationDetails, periodKey: String) =>
        val openObligation              = obligationDetails.copy(status = Open)
        val returnWithObligationDetails =
          EclReturn.empty(internalId, Some(FirstTimeReturn)).copy(obligationDetails = Some(openObligation))
        new TestContext(returnWithObligationDetails, periodKey) {

          when(mockEclReturnsService.getOrCreateReturn(any(), any())(any(), any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, EclReturn](Future.successful(Right(returnWithObligationDetails)))
            )

          val result: Future[Result] = controller.start()(fakeRequest)

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.StartController.onPageLoad(obligationDetails.periodKey).url)
        }
    }

    "show the choose return period view if the return data does not contain any obligation details" in forAll {
      (periodKey: String) =>
        val returnWithoutObligationDetails = EclReturn.empty(internalId, Some(FirstTimeReturn))
        new TestContext(returnWithoutObligationDetails, periodKey) {
          when(mockEclReturnsService.getOrCreateReturn(any(), any())(any(), any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, EclReturn](Future.successful(Right(returnWithoutObligationDetails)))
            )

          val result: Future[Result] = controller.start()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe chooseReturnPeriodView()(fakeRequest, messages).toString
        }
    }
  }

  "onPageLoad" should {
    "upsert the return data to contain the obligation details if the same period key is already held or no obligation details exist" +
      "and return OK with the start view when the period key is for an open obligation" in forAll {
        (internalId: String, obligationDetails: ObligationDetails, eclRegistrationDate: LocalDate) =>
          val openObligation = obligationDetails.copy(status = Open, periodKey = periodKey)
          val updatedReturn  =
            EclReturn.empty(internalId, Some(FirstTimeReturn)).copy(obligationDetails = Some(openObligation))
          new TestContext(updatedReturn, periodKey) {
            when(
              mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(
                any()
              )
            )
              .thenReturn(EitherT[Future, DataHandlingError, LocalDate](Future.successful(Right(eclRegistrationDate))))

            when(mockEclReturnsService.getOrCreateReturn(any(), any())(any(), any()))
              .thenReturn(
                EitherT[Future, DataHandlingError, EclReturn](
                  Future.successful(Right(EclReturn.empty(internalId, Some(FirstTimeReturn))))
                )
              )

            when(mockSessionService.upsert(any())(any()))
              .thenReturn(EitherT.fromEither[Future](Right(())))

            when(
              mockEclReturnsService.upsertReturn(
                ArgumentMatchers.eq(updatedReturn)
              )(any())
            ).thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

            val result: Future[Result] = controller.onPageLoad(openObligation.periodKey)(fakeRequest)

            status(result) shouldBe OK

            contentAsString(result) shouldBe view(
              eclRegistrationReference,
              ViewUtils.formatLocalDate(eclRegistrationDate)(messages),
              ViewUtils.formatObligationPeriodYears(openObligation)
            )(fakeRequest, messages).toString
          }
      }

    "upsert the return data to contain the obligation details, clearing any existing return data if the period key does not match one already held" +
      "and return OK with the start view when the period key is for an open obligation" in forAll {
        (internalId: String, obligationDetails: ObligationDetails, eclRegistrationDate: LocalDate) =>
          val openObligation                       = obligationDetails.copy(status = Open, periodKey = "P1")
          val existingReturnWithDifferentPeriodKey =
            EclReturn
              .empty(internalId, Some(FirstTimeReturn))
              .copy(obligationDetails = Some(openObligation.copy(periodKey = "P2")))
          new TestContext(existingReturnWithDifferentPeriodKey, "P2") {
            when(
              mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(
                any()
              )
            )
              .thenReturn(EitherT[Future, DataHandlingError, LocalDate](Future.successful(Right(eclRegistrationDate))))

            when(mockEclReturnsService.getOrCreateReturn(any(), any())(any(), any()))
              .thenReturn(
                EitherT[Future, DataHandlingError, EclReturn](
                  Future.successful(Right(existingReturnWithDifferentPeriodKey))
                )
              )

            when(mockEclReturnsService.deleteReturn(any())(any()))
              .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

            when(
              mockEclReturnsService.upsertReturn(
                any()
              )(any())
            ).thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

            when(mockSessionService.upsert(any())(any()))
              .thenReturn(EitherT.fromEither[Future](Right(())))

            val result: Future[Result] = controller.onPageLoad(openObligation.periodKey)(fakeRequest)

            status(result) shouldBe OK

            contentAsString(result) shouldBe view(
              eclRegistrationReference,
              ViewUtils.formatLocalDate(eclRegistrationDate)(messages),
              ViewUtils.formatObligationPeriodYears(openObligation)
            )(fakeRequest, messages).toString
          }
      }

    "return OK and no obligation for period view when a period key is specified where there is no obligation" in forAll {
      (eclRegistrationDate: LocalDate, eclReturn: EclReturn) =>
        val noObligationReturn = eclReturn.copy(obligationDetails = None)
        new TestContext(noObligationReturn, periodKey) {
          when(
            mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
          )
            .thenReturn(EitherT[Future, DataHandlingError, LocalDate](Future.successful(Right(eclRegistrationDate))))

          when(mockSessionService.upsert(any())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(())))

          val result: Future[Result] = controller.onPageLoad(periodKey)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe noObligationForPeriodView()(fakeRequest, messages).toString
        }
    }

    "return OK and the already submitted return view when a period key is specified for an obligation that is already fulfilled" in forAll {
      (obligationDetails: ObligationDetails, eclRegistrationDate: LocalDate, eclReturn: EclReturn) =>
        val today               = LocalDate.now()
        val fulfilledObligation =
          obligationDetails.copy(status = Fulfilled, inboundCorrespondenceDateReceived = Some(today))
        val updatedReturn       = eclReturn.copy(obligationDetails = Some(fulfilledObligation))
        new TestContext(updatedReturn, periodKey) {
          when(
            mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
          )
            .thenReturn(EitherT[Future, DataHandlingError, LocalDate](Future.successful(Right(eclRegistrationDate))))

          when(mockSessionService.upsert(any())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(())))

          val result: Future[Result] = controller.onPageLoad(fulfilledObligation.periodKey)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe alreadySubmittedReturnView(
            obligationDetails.inboundCorrespondenceFromDate.getYear.toString,
            obligationDetails.inboundCorrespondenceToDate.getYear.toString,
            ViewUtils.formatLocalDate(today)(messages)
          )(fakeRequest, messages).toString
        }
    }

    "return InternalServerError when a fulfilled obligation does not contain an inboundCorrespondenceDateReceived" in forAll {
      (obligationDetails: ObligationDetails, eclRegistrationDate: LocalDate, eclReturn: EclReturn) =>
        val fulfilledObligation =
          obligationDetails.copy(status = Fulfilled, inboundCorrespondenceDateReceived = None, periodKey = periodKey)
        val updatedReturn       = eclReturn.copy(obligationDetails = Some(fulfilledObligation))
        new TestContext(updatedReturn, periodKey) {
          when(
            mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
          )
            .thenReturn(EitherT[Future, DataHandlingError, LocalDate](Future.successful(Right(eclRegistrationDate))))

          when(mockSessionService.upsert(any())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(())))

          val result: Future[Result] = controller.onPageLoad(fulfilledObligation.periodKey)(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }
  }

  "onSubmit" should {
    "redirect to Relevant accounting period page if no return url" in forAll { (eclReturn: EclReturn) =>
      new TestContext(eclReturn, periodKey) {
        when(mockSessionService.getOptional(any(), any(), ArgumentMatchers.eq(SessionKeys.urlToReturnTo))(any()))
          .thenReturn(EitherT.fromEither[Future](Right(None)))

        when(mockSessionService.getOptional(any(), any(), ArgumentMatchers.eq(SessionKeys.periodKey))(any()))
          .thenReturn(EitherT.fromEither[Future](Right(Some("period-key"))))

        val result: Future[Result] = controller.onSubmit()(fakeRequest)

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.RelevantAp12MonthsController.onPageLoad(NormalMode).url)
      }
    }

    "redirect to Saved Responses page if there is a return url" in forAll { (eclReturn: EclReturn) =>
      new TestContext(eclReturn, periodKey) {
        when(mockSessionService.getOptional(any(), any(), ArgumentMatchers.eq(SessionKeys.urlToReturnTo))(any()))
          .thenReturn(EitherT.fromEither[Future](Right(Some(random[String]))))

        when(mockSessionService.getOptional(any(), any(), ArgumentMatchers.eq(SessionKeys.periodKey))(any()))
          .thenReturn(EitherT.fromEither[Future](Right(Some("period-key"))))

        val result: Future[Result] = controller.onSubmit()(fakeRequest)

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.SavedResponsesController.onPageLoad().url)
      }
    }
  }
}
