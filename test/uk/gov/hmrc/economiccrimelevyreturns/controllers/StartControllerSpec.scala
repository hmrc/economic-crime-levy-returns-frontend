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
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.{EclAccountConnector, EclReturnsConnector}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclReturnsService, EnrolmentStoreProxyService}
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AlreadySubmittedReturnView, ChooseReturnPeriodView, NoObligationForPeriodView, StartView}

import java.time.LocalDate
import scala.concurrent.Future

class StartControllerSpec extends SpecBase {

  val mockEnrolmentStoreProxyService: EnrolmentStoreProxyService = mock[EnrolmentStoreProxyService]
  val mockEclAccountConnector: EclAccountConnector               = mock[EclAccountConnector]
  val mockEclReturnsService: EclReturnsService                   = mock[EclReturnsService]
  val mockEclReturnsConnector: EclReturnsConnector               = mock[EclReturnsConnector]

  val view: StartView                                        = app.injector.instanceOf[StartView]
  val alreadySubmittedReturnView: AlreadySubmittedReturnView = app.injector.instanceOf[AlreadySubmittedReturnView]
  val noObligationForPeriodView: NoObligationForPeriodView   = app.injector.instanceOf[NoObligationForPeriodView]
  val chooseReturnPeriodView: ChooseReturnPeriodView         = app.injector.instanceOf[ChooseReturnPeriodView]

  val controller = new StartController(
    mcc,
    fakeAuthorisedAction(internalId),
    mockEnrolmentStoreProxyService,
    mockEclAccountConnector,
    mockEclReturnsService,
    mockEclReturnsConnector,
    alreadySubmittedReturnView,
    noObligationForPeriodView,
    chooseReturnPeriodView,
    view
  )

  "start" should {
    "redirect to the start page if the return data contains obligation details" in forAll {
      (internalId: String, obligationDetails: ObligationDetails) =>
        val openObligation = obligationDetails.copy(status = Open)

        val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))

        when(mockEclAccountConnector.getObligations()(any())).thenReturn(Future.successful(Some(obligationData)))

        val returnWithObligationDetails = EclReturn.empty(internalId).copy(obligationDetails = Some(openObligation))

        when(mockEclReturnsService.getOrCreateReturn(any())(any(), any()))
          .thenReturn(Future.successful(returnWithObligationDetails))

        val result: Future[Result] = controller.start()(fakeRequest)

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.StartController.onPageLoad(obligationDetails.periodKey).url)
    }

    "show the choose return period view if the return data does not contain any obligation details" in {
      val returnWithoutObligationDetails = EclReturn.empty(internalId)

      when(mockEclReturnsService.getOrCreateReturn(any())(any(), any()))
        .thenReturn(Future.successful(returnWithoutObligationDetails))

      val result: Future[Result] = controller.start()(fakeRequest)

      status(result) shouldBe OK

      contentAsString(result) shouldBe chooseReturnPeriodView()(fakeRequest, messages).toString
    }
  }

  "onPageLoad" should {
    "upsert the return data to contain the obligation details if the same period key is already held or no obligation details exist" +
      "and return OK with the start view when the period key is for an open obligation" in forAll {
        (internalId: String, obligationDetails: ObligationDetails, eclRegistrationDate: LocalDate) =>
          when(
            mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
          )
            .thenReturn(Future.successful(eclRegistrationDate))

          val openObligation = obligationDetails.copy(status = Open)

          val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))

          when(mockEclAccountConnector.getObligations()(any())).thenReturn(Future.successful(Some(obligationData)))

          when(mockEclReturnsService.getOrCreateReturn(any())(any(), any()))
            .thenReturn(Future.successful(EclReturn.empty(internalId)))

          val updatedReturn = EclReturn.empty(internalId).copy(obligationDetails = Some(openObligation))

          when(
            mockEclReturnsConnector.upsertReturn(
              ArgumentMatchers.eq(updatedReturn)
            )(any())
          )
            .thenReturn(Future.successful(updatedReturn))

          val result: Future[Result] = controller.onPageLoad(openObligation.periodKey)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            eclRegistrationReference,
            ViewUtils.formatLocalDate(eclRegistrationDate)(messages),
            ViewUtils.formatObligationPeriodYears(obligationDetails)
          )(fakeRequest, messages).toString
      }

    "upsert the return data to contain the obligation details, clearing any existing return data if the period key does not match one already held " +
      "and return OK with the start view when the period key is for an open obligation" in forAll {
        (internalId: String, obligationDetails: ObligationDetails, eclRegistrationDate: LocalDate) =>
          when(
            mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
          )
            .thenReturn(Future.successful(eclRegistrationDate))

          val openObligation = obligationDetails.copy(status = Open, periodKey = "P1")

          val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))

          when(mockEclAccountConnector.getObligations()(any())).thenReturn(Future.successful(Some(obligationData)))

          val existingReturnWithDifferentPeriodKey =
            EclReturn.empty(internalId).copy(obligationDetails = Some(openObligation.copy(periodKey = "P2")))

          when(mockEclReturnsService.getOrCreateReturn(any())(any(), any()))
            .thenReturn(Future.successful(existingReturnWithDifferentPeriodKey))

          val updatedReturn = EclReturn.empty(internalId).copy(obligationDetails = Some(openObligation))

          when(mockEclReturnsConnector.deleteReturn(any())(any())).thenReturn(Future.successful(()))

          when(
            mockEclReturnsConnector.upsertReturn(
              ArgumentMatchers.eq(updatedReturn)
            )(any())
          )
            .thenReturn(Future.successful(updatedReturn))

          val result: Future[Result] = controller.onPageLoad(openObligation.periodKey)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            eclRegistrationReference,
            ViewUtils.formatLocalDate(eclRegistrationDate)(messages),
            ViewUtils.formatObligationPeriodYears(obligationDetails)
          )(fakeRequest, messages).toString
      }

    "return OK and the no obligation for period view when a period key is specified where there is no obligation" in forAll {
      (eclRegistrationDate: LocalDate, periodKey: String) =>
        when(
          mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
        )
          .thenReturn(Future.successful(eclRegistrationDate))

        when(mockEclAccountConnector.getObligations()(any())).thenReturn(Future.successful(None))

        val result: Future[Result] = controller.onPageLoad(periodKey)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe noObligationForPeriodView()(fakeRequest, messages).toString
    }

    "return OK and the already submitted return view when a period key is specified for an obligation that is already fulfilled" in forAll {
      (obligationDetails: ObligationDetails, eclRegistrationDate: LocalDate) =>
        when(
          mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
        )
          .thenReturn(Future.successful(eclRegistrationDate))

        val today = LocalDate.now()

        val fulfilledObligation =
          obligationDetails.copy(status = Fulfilled, inboundCorrespondenceDateReceived = Some(today))

        val obligationData = ObligationData(obligations = Seq(Obligation(Seq(fulfilledObligation))))

        when(mockEclAccountConnector.getObligations()(any())).thenReturn(Future.successful(Some(obligationData)))

        val result: Future[Result] = controller.onPageLoad(fulfilledObligation.periodKey)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe alreadySubmittedReturnView(
          obligationDetails.inboundCorrespondenceFromDate.getYear.toString,
          obligationDetails.inboundCorrespondenceToDate.getYear.toString,
          ViewUtils.formatLocalDate(today)(messages)
        )(fakeRequest, messages).toString
    }

    "throw an IllegalStateException when a fulfilled obligation does not contain an inboundCorrespondenceDateReceived" in forAll {
      (obligationDetails: ObligationDetails, eclRegistrationDate: LocalDate) =>
        when(
          mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
        )
          .thenReturn(Future.successful(eclRegistrationDate))

        val fulfilledObligation =
          obligationDetails.copy(status = Fulfilled, inboundCorrespondenceDateReceived = None)

        val obligationData = ObligationData(obligations = Seq(Obligation(Seq(fulfilledObligation))))

        when(mockEclAccountConnector.getObligations()(any())).thenReturn(Future.successful(Some(obligationData)))

        val result = intercept[IllegalStateException] {
          await(controller.onPageLoad(fulfilledObligation.periodKey)(fakeRequest))
        }

        result.getMessage shouldBe "Fulfilled obligation does not have an inboundCorrespondenceDateReceived"
    }
  }

}
