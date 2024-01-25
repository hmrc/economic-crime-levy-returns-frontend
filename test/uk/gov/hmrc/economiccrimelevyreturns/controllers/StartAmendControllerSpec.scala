/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.data.Form
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.forms.AmendReasonFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, EclAccountError, LiabilityCalculationError}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclAccountService, EclCalculatorService, ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AmendReasonView, NoObligationForPeriodView, StartAmendView}
import uk.gov.hmrc.economiccrimelevyreturns.{ValidEclReturn, ValidGetEclReturnSubmissionResponse}

import scala.concurrent.Future
import scala.concurrent.Future.unit

class StartAmendControllerSpec extends SpecBase {

  val mockEclAccountService: EclAccountService      = mock[EclAccountService]
  val mockEclReturnsService: ReturnsService         = mock[ReturnsService]
  val mockSessionService: SessionService            = mock[SessionService]
  val mockEclLiabilityService: EclCalculatorService = mock[EclCalculatorService]

  val formProvider: AmendReasonFormProvider                = new AmendReasonFormProvider()
  val form: Form[String]                                   = formProvider()
  val view: StartAmendView                                 = app.injector.instanceOf[StartAmendView]
  val amendReasonView: AmendReasonView                     = app.injector.instanceOf[AmendReasonView]
  val noObligationForPeriodView: NoObligationForPeriodView = app.injector.instanceOf[NoObligationForPeriodView]

  val controller = new StartAmendController(
    controllerComponents = mcc,
    authorise = fakeAuthorisedAction(internalId),
    eclAccountService = mockEclAccountService,
    returnsService = mockEclReturnsService,
    sessionService = mockSessionService,
    noObligationForPeriodView = noObligationForPeriodView,
    view = view,
    appConfig,
    mockEclLiabilityService
  )

  "onPageLoad" should {
    "redirect to amend reason screen" in forAll {
      (obligationDetails: ObligationDetails,
       validEclReturn: ValidEclReturn,
       validGetEclReturnSubmission: ValidGetEclReturnSubmissionResponse,
       calculatedLiability: CalculatedLiability,
      ) =>

        val openObligation = obligationDetails.copy(status = Open, periodKey = periodKey)
        val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))

        val validAmendReturn = validEclReturn.copy(
          eclReturn = validEclReturn.eclReturn.copy(obligationDetails = Some(openObligation),
            returnType = Some(AmendReturn)))

        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(
            EitherT[Future, EclAccountError, Option[ObligationData]](Future.successful(Right(Some(obligationData))))
          )

        when(mockEclReturnsService.getOrCreateReturn(any(), any())(any(), any()))
          .thenReturn(
            EitherT[Future, DataHandlingError, EclReturn](
              Future.successful(Right(validAmendReturn.eclReturn))
            )
          )

        when(mockEclReturnsService.getReturn(any())(any()))
          .thenReturn(
            EitherT[Future, DataHandlingError, Option[EclReturn]](
              Future.successful(Right(Some(validAmendReturn.eclReturn)))
            )
          )

        when(mockEclReturnsService.upsertReturn(any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        when(mockEclReturnsService.getEclReturnSubmission(any(), any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, GetEclReturnSubmissionResponse](Future.successful(Right(validGetEclReturnSubmission.response))))

        when(mockEclReturnsService.transformEclReturnSubmissionToEclReturn(any(), any(),any()))
          .thenReturn(Right(validEclReturn.eclReturn))

        when(mockSessionService.upsert(any())(any()))
          .thenReturn(unit)

        when(mockEclLiabilityService.getCalculatedLiability(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, LiabilityCalculationError, CalculatedLiability](Future.successful(Right(calculatedLiability))))

        val authorisedRequest: AuthorisedRequest[AnyContent] =
          AuthorisedRequest.apply(fakeRequest, validEclReturn.eclReturn.internalId, validGetEclReturnSubmission.response.eclReference)

        val result: Future[Result] =
          controller.onPageLoad(periodKey = openObligation.periodKey, returnNumber = "ABC")(authorisedRequest)

        status(result) shouldBe SEE_OTHER
    }

    "return No Obligation view when there is no obligation returned" in forAll {
      (periodKey: String, returnNumber: String) =>
        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT[Future, EclAccountError, Option[ObligationData]](Future.successful(Right(None))))

        when(mockSessionService.upsert(any())(any()))
          .thenReturn(unit)

        val result: Future[Result] = controller.onPageLoad(periodKey, returnNumber)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe noObligationForPeriodView()(fakeRequest, messages).toString()
    }

    "return INTERNAL_SERVER_ERROR when retrieveObligationData fails" in forAll { (eclReturnNumber: String) =>
      when(mockEclAccountService.retrieveObligationData(any()))
        .thenReturn(
          EitherT[Future, EclAccountError, Option[ObligationData]](
            Future.successful(Left(EclAccountError.InternalUnexpectedError(None, None)))
          )
        )

      val result = controller.onPageLoad(periodKey, eclReturnNumber)(fakeRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR when returnsService.getOrCreateReturn fails" in forAll {
      (obligationDetails: ObligationDetails) =>
        val openObligation = obligationDetails.copy(status = Open, periodKey = periodKey)
        val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))

        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(
            EitherT[Future, EclAccountError, Option[ObligationData]](Future.successful(Right(Some(obligationData))))
          )

        when(mockEclReturnsService.getOrCreateReturn(any(), any())(any(), any()))
          .thenReturn(
            EitherT[Future, DataHandlingError, EclReturn](
              Future.successful(Left(DataHandlingError.InternalUnexpectedError(None, None)))
            )
          )

        val result = controller.onPageLoad(periodKey, eclReturnReference)(fakeRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR when returnsService.getEclReturnSubmission fails" in forAll {
      (obligationDetails: ObligationDetails, validEclReturn: ValidEclReturn) =>

        val openObligation = obligationDetails.copy(status = Open, periodKey = periodKey)
        val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))

        val validAmendReturn = validEclReturn.copy(
          eclReturn = validEclReturn.eclReturn.copy(obligationDetails = Some(openObligation),
            returnType = Some(AmendReturn)))

        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(
            EitherT[Future, EclAccountError, Option[ObligationData]](Future.successful(Right(Some(obligationData))))
          )

        when(mockEclReturnsService.getOrCreateReturn(any(), any())(any(), any()))
          .thenReturn(
            EitherT[Future, DataHandlingError, EclReturn](
              Future.successful(Right(validAmendReturn.eclReturn))
            )
          )

        when(mockEclReturnsService.getReturn(any())(any()))
          .thenReturn(
            EitherT[Future, DataHandlingError, Option[EclReturn]](
              Future.successful(Right(Some(validAmendReturn.eclReturn)))
            )
          )

        when(mockEclReturnsService.upsertReturn(any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        when(mockEclReturnsService.getEclReturnSubmission(any(), any())(any()))
          .thenReturn(
            EitherT[Future, DataHandlingError, GetEclReturnSubmissionResponse](
              Future.successful(Left(DataHandlingError.InternalUnexpectedError(None, None)))
            )
          )

        val result = controller.onPageLoad(periodKey, eclReturnReference)(fakeRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR when eclCalculatorService.getCalculatedLiability fails" in forAll {
      (obligationDetails: ObligationDetails,
       validEclReturn: ValidEclReturn,
       validGetEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>

        val openObligation = obligationDetails.copy(status = Open, periodKey = periodKey)
        val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))

        val validAmendReturn = validEclReturn.copy(
          eclReturn = validEclReturn.eclReturn.copy(obligationDetails = Some(openObligation),
            returnType = Some(AmendReturn)))

        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(
            EitherT[Future, EclAccountError, Option[ObligationData]](Future.successful(Right(Some(obligationData))))
          )

        when(mockEclReturnsService.getOrCreateReturn(any(), any())(any(), any()))
          .thenReturn(
            EitherT[Future, DataHandlingError, EclReturn](
              Future.successful(Right(validAmendReturn.eclReturn))
            )
          )

        when(mockEclReturnsService.getReturn(any())(any()))
          .thenReturn(
            EitherT[Future, DataHandlingError, Option[EclReturn]](
              Future.successful(Right(Some(validAmendReturn.eclReturn)))
            )
          )

        when(mockEclReturnsService.upsertReturn(any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        when(mockEclReturnsService.getEclReturnSubmission(any(), any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, GetEclReturnSubmissionResponse](Future.successful(Right(validGetEclReturnSubmission.response))))

        when(mockEclLiabilityService.getCalculatedLiability(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, LiabilityCalculationError, CalculatedLiability](Future.successful(Left(LiabilityCalculationError.InternalUnexpectedError(None, None)))))

        val result = controller.onPageLoad(periodKey, eclReturnReference)(fakeRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR when returnsService.upsertReturn fails" in forAll {
      (obligationDetails: ObligationDetails,
       validEclReturn: ValidEclReturn,
       validGetEclReturnSubmission: ValidGetEclReturnSubmissionResponse,
       calculatedLiability: CalculatedLiability) =>

        val openObligation = obligationDetails.copy(status = Open, periodKey = periodKey)
        val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))

        val validAmendReturn = validEclReturn.copy(
          eclReturn = validEclReturn.eclReturn.copy(obligationDetails = Some(openObligation),
            returnType = Some(AmendReturn)))

        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(
            EitherT[Future, EclAccountError, Option[ObligationData]](Future.successful(Right(Some(obligationData))))
          )

        when(mockEclReturnsService.getOrCreateReturn(any(), any())(any(), any()))
          .thenReturn(
            EitherT[Future, DataHandlingError, EclReturn](
              Future.successful(Right(validAmendReturn.eclReturn))
            )
          )

        when(mockEclReturnsService.getReturn(any())(any()))
          .thenReturn(
            EitherT[Future, DataHandlingError, Option[EclReturn]](
              Future.successful(Right(Some(validAmendReturn.eclReturn)))
            )
          )

        when(mockEclReturnsService.upsertReturn(any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        when(mockEclReturnsService.getEclReturnSubmission(any(), any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, GetEclReturnSubmissionResponse](Future.successful(Right(validGetEclReturnSubmission.response))))

        when(mockEclLiabilityService.getCalculatedLiability(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, LiabilityCalculationError, CalculatedLiability](Future.successful(Right(calculatedLiability))))

        when(mockEclReturnsService.upsertReturn(any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Left(DataHandlingError.InternalUnexpectedError(None, None)))))

        val result = controller.onPageLoad(periodKey, eclReturnReference)(fakeRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
