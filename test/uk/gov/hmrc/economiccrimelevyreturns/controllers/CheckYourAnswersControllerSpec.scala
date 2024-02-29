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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, DataValidationError}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.services.{EmailService, RegistrationService, ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers._
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AmendReturnPdfView, CheckYourAnswersView}
import uk.gov.hmrc.economiccrimelevyreturns.{ValidEclReturn, ValidGetEclReturnSubmissionResponse}

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  val view: CheckYourAnswersView        = app.injector.instanceOf[CheckYourAnswersView]
  val pdfReturnView: AmendReturnPdfView = app.injector.instanceOf[AmendReturnPdfView]

  val mockEclReturnsService: ReturnsService        = mock[ReturnsService]
  val mockSessionService: SessionService           = mock[SessionService]
  val mockEmailService: EmailService               = mock[EmailService]
  val mockRegistrationService: RegistrationService = mock[RegistrationService]

  class TestContext(eclReturnData: EclReturn, periodKey: Option[String] = None) {
    val controller = new CheckYourAnswersController(
      messagesApi,
      fakeAuthorisedAction(eclReturnData.internalId),
      fakeDataRetrievalAction(eclReturnData, periodKey),
      mockEclReturnsService,
      mockSessionService,
      mockEmailService,
      pdfReturnView,
      mcc,
      view,
      appConfig,
      fakeNoOpStoreUrlAction,
      mockRegistrationService
    )
  }

  private def createTestEclReturnSubmission(
    validEclReturn: ValidEclReturn,
    validEclSubmission: ValidGetEclReturnSubmissionResponse
  ): GetEclReturnSubmissionResponse = {
    val eclReturn: EclReturn = validEclReturn.eclReturn

    validEclSubmission.response.copy(
      declarationDetails = GetEclReturnDeclarationDetails(
        emailAddress = eclReturn.contactEmailAddress.getOrElse(""),
        name = eclReturn.contactName.getOrElse(""),
        positionInCompany = eclReturn.contactRole.getOrElse(""),
        telephoneNumber = eclReturn.contactTelephoneNumber.getOrElse("")
      )
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll {
      (
        validEclReturn: ValidEclReturn,
        validEclSubmission: ValidGetEclReturnSubmissionResponse
      ) =>
        val eclReturn: EclReturn                                = validEclReturn.eclReturn
        val eclReturnSubmission: GetEclReturnSubmissionResponse =
          createTestEclReturnSubmission(validEclReturn, validEclSubmission)

        new TestContext(eclReturn, Some(periodKey)) {
          implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
            ReturnDataRequest(
              fakeRequest,
              eclReturn.internalId,
              eclReturn,
              None,
              eclRegistrationReference,
              Some(periodKey)
            )

          implicit val messages: Messages = messagesApi.preferred(returnDataRequest)

          when(mockEclReturnsService.getReturnValidationErrors(any())(any()))
            .thenReturn(EitherT[Future, DataHandlingError, Option[DataValidationError]](Future.successful(Right(None))))

          when(mockEclReturnsService.getEclReturnSubmission(any(), any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetEclReturnSubmissionResponse](
                Future.successful(Right(eclReturnSubmission))
              )
            )

          val viewModel: CheckYourAnswersViewModel = CheckYourAnswersViewModel(returnDataRequest.eclReturn, None, None)

          val result: Future[Result] = controller.onPageLoad()(returnDataRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(viewModel, appConfig)(
            returnDataRequest,
            messages
          ).toString
        }
    }

    "return answers are invalid page when answers are invalid" in forAll { (validEclReturn: ValidEclReturn) =>
      val eclReturn: EclReturn = validEclReturn.eclReturn

      new TestContext(eclReturn, Some(periodKey)) {
        implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
          ReturnDataRequest(
            fakeRequest,
            eclReturn.internalId,
            eclReturn,
            None,
            eclRegistrationReference,
            Some(periodKey)
          )

        val validationErrors: DataValidationError = DataValidationError.apply("Error")

        when(mockEclReturnsService.getReturnValidationErrors(any())(any()))
          .thenReturn(
            EitherT[Future, DataHandlingError, Option[DataValidationError]](
              Future.successful(Right(Some(validationErrors)))
            )
          )

        val result: Future[Result] = controller.onPageLoad()(returnDataRequest)

        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
      }
    }

    "return InternalServerError (500) when getReturnValidationErrors errors" in forAll {
      validEclReturn: ValidEclReturn =>
        new TestContext(validEclReturn.eclReturn) {
          implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
            ReturnDataRequest(
              fakeRequest,
              validEclReturn.eclReturn.internalId,
              validEclReturn.eclReturn,
              None,
              eclRegistrationReference,
              None
            )

          when(mockEclReturnsService.getReturnValidationErrors(any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, Option[DataValidationError]](
                Future.successful(Left(DataHandlingError.InternalUnexpectedError(None, None)))
              )
            )

          val result: Future[Result] = controller.onPageLoad()(returnDataRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "return InternalServerError (500) when periodKey is missing" in forAll { validEclReturn: ValidEclReturn =>
      val eclReturn = validEclReturn.eclReturn.copy(returnType = Some(AmendReturn))

      new TestContext(eclReturn) {
        implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
          ReturnDataRequest(
            fakeRequest,
            eclReturn.internalId,
            eclReturn,
            None,
            eclRegistrationReference,
            None
          )

        when(mockEclReturnsService.getReturnValidationErrors(any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Option[DataValidationError]](Future.successful(Right(None))))

        val result: Future[Result] = controller.onPageLoad()(returnDataRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "return InternalServerError (500) when getEclReturnSubmission errors" in forAll {
      (validEclReturn: ValidEclReturn) =>
        val eclReturn = validEclReturn.eclReturn.copy(returnType = Some(AmendReturn))

        new TestContext(eclReturn, Some(periodKey)) {
          implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
            ReturnDataRequest(
              fakeRequest,
              eclReturn.internalId,
              eclReturn,
              None,
              eclRegistrationReference,
              Some(periodKey)
            )

          when(mockEclReturnsService.getReturnValidationErrors(any())(any()))
            .thenReturn(EitherT[Future, DataHandlingError, Option[DataValidationError]](Future.successful(Right(None))))

          when(mockEclReturnsService.getEclReturnSubmission(any(), any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetEclReturnSubmissionResponse](
                Future.successful(Left(DataHandlingError.InternalUnexpectedError(None, None)))
              )
            )

          val result: Future[Result] = controller.onPageLoad()(returnDataRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }
  }

  "onSubmit" should {
    "redirect to the ECL return submitted page after submitting the ECL return successfully" in forAll {
      (
        validEclReturn: ValidEclReturn,
        validEclSubmission: ValidGetEclReturnSubmissionResponse,
        submitEclReturnResponse: SubmitEclReturnResponse,
        subscriptionResponse: GetSubscriptionResponse
      ) =>
        val eclReturn: EclReturn                                = validEclReturn.eclReturn
        val eclReturnSubmission: GetEclReturnSubmissionResponse =
          createTestEclReturnSubmission(validEclReturn, validEclSubmission)

        new TestContext(eclReturn, Some(periodKey)) {
          implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
            ReturnDataRequest(
              fakeRequest,
              eclReturn.internalId,
              eclReturn,
              None,
              eclRegistrationReference,
              Some(periodKey)
            )

          when(mockEclReturnsService.getEclReturnSubmission(any(), any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetEclReturnSubmissionResponse](
                Future.successful(Right(eclReturnSubmission))
              )
            )

          when(mockRegistrationService.getSubscription(any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetSubscriptionResponse](
                Future.successful(Right(subscriptionResponse))
              )
            )

          when(mockEclReturnsService.upsertReturn(any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, Unit](Future.successful(Right(())))
            )

          when(mockEclReturnsService.submitReturn(ArgumentMatchers.eq(eclReturn.internalId))(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, SubmitEclReturnResponse](
                Future.successful(Right(submitEclReturnResponse))
              )
            )

          val result: Future[Result] = controller.onSubmit()(returnDataRequest)

          status(result)                                     shouldBe SEE_OTHER
          session(result).get(SessionKeys.ChargeReference)   shouldBe submitEclReturnResponse.chargeReference
          session(result).get(SessionKeys.Email)             shouldBe eclReturn.contactEmailAddress
          session(result).get(SessionKeys.ObligationDetails) shouldBe Some(
            Json.toJson(eclReturn.obligationDetails.get).toString()
          )
          session(result).get(SessionKeys.AmountDue)         shouldBe Some(
            eclReturn.calculatedLiability.get.amountDue.amount.toString()
          )
          redirectLocation(result)                           shouldBe Some(routes.ReturnSubmittedController.onPageLoad().url)

          verify(mockEmailService, times(1)).sendReturnSubmittedEmail(
            ArgumentMatchers.eq(eclReturn),
            ArgumentMatchers.eq(submitEclReturnResponse.chargeReference)
          )(any(), any())

          reset(mockEmailService)
        }
    }

    "redirect to answers not valid page when the contact email is not present in the ECL return" in forAll {
      (
        validEclReturn: ValidEclReturn,
        validEclSubmission: ValidGetEclReturnSubmissionResponse,
        submitEclReturnResponse: SubmitEclReturnResponse,
        subscriptionResponse: GetSubscriptionResponse
      ) =>
        val eclReturn: EclReturn                                = validEclReturn.eclReturn.copy(contactEmailAddress = None)
        val eclReturnSubmission: GetEclReturnSubmissionResponse =
          createTestEclReturnSubmission(validEclReturn, validEclSubmission)

        new TestContext(eclReturn, Some(periodKey)) {
          implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
            ReturnDataRequest(
              fakeRequest,
              eclReturn.internalId,
              eclReturn,
              None,
              eclRegistrationReference,
              Some(periodKey)
            )

          when(mockEclReturnsService.getEclReturnSubmission(any(), any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetEclReturnSubmissionResponse](
                Future.successful(Right(eclReturnSubmission))
              )
            )

          when(mockRegistrationService.getSubscription(any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetSubscriptionResponse](
                Future.successful(Right(subscriptionResponse))
              )
            )

          when(mockEclReturnsService.upsertReturn(any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, Unit](Future.successful(Right(())))
            )

          when(mockEclReturnsService.submitReturn(ArgumentMatchers.eq(eclReturn.internalId))(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, SubmitEclReturnResponse](
                Future.successful(Right(submitEclReturnResponse))
              )
            )

          val result: Future[Result] = controller.onSubmit()(returnDataRequest)

          status(result)           shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)

          verify(mockEmailService, times(1)).sendReturnSubmittedEmail(
            ArgumentMatchers.eq(eclReturn),
            ArgumentMatchers.eq(submitEclReturnResponse.chargeReference)
          )(any(), any())

          reset(mockEmailService)
        }
    }

    "return InternalServerError (500) when periodKey is missing for viewHtml" in forAll {
      validEclReturn: ValidEclReturn =>
        val eclReturn = validEclReturn.eclReturn.copy(returnType = Some(AmendReturn))

        new TestContext(eclReturn) {
          implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
            ReturnDataRequest(
              fakeRequest,
              eclReturn.internalId,
              eclReturn,
              None,
              eclRegistrationReference,
              None
            )

          when(mockEclReturnsService.getReturnValidationErrors(any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, Option[DataValidationError]](Future.successful(Right(None)))
            )

          val result: Future[Result] = controller.onSubmit()(returnDataRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "return InternalServerError (500) when getEclReturnSubmission fails for pdfViewHtml" in forAll {
      (
        validEclReturn: ValidEclReturn,
        validEclSubmission: ValidGetEclReturnSubmissionResponse,
        subscriptionResponse: GetSubscriptionResponse
      ) =>
        val eclReturn: EclReturn                                = validEclReturn.eclReturn
          .copy(returnType = Some(AmendReturn))
        val eclReturnSubmission: GetEclReturnSubmissionResponse =
          createTestEclReturnSubmission(validEclReturn, validEclSubmission)

        new TestContext(eclReturn, Some(periodKey)) {
          implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
            ReturnDataRequest(
              fakeRequest,
              eclReturn.internalId,
              eclReturn,
              None,
              eclRegistrationReference,
              Some(periodKey)
            )

          when(mockEclReturnsService.getReturnValidationErrors(any())(any()))
            .thenReturn(EitherT[Future, DataHandlingError, Option[DataValidationError]](Future.successful(Right(None))))

          when(mockRegistrationService.getSubscription(any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetSubscriptionResponse](
                Future.successful(Right(subscriptionResponse))
              )
            )

          when(mockEclReturnsService.getEclReturnSubmission(any(), any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetEclReturnSubmissionResponse](
                Future.successful(Right(eclReturnSubmission))
              ),
              EitherT[Future, DataHandlingError, GetEclReturnSubmissionResponse](
                Future.successful(Left(DataHandlingError.InternalUnexpectedError(None, None)))
              )
            )

          val result: Future[Result] = controller.onSubmit()(returnDataRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "return InternalServerError (500) when returnsService.upsertReturn fails" in forAll {
      (
        validEclReturn: ValidEclReturn,
        validEclSubmission: ValidGetEclReturnSubmissionResponse,
        subscriptionResponse: GetSubscriptionResponse
      ) =>
        val eclReturn: EclReturn                                = validEclReturn.eclReturn
        val eclReturnSubmission: GetEclReturnSubmissionResponse =
          createTestEclReturnSubmission(validEclReturn, validEclSubmission)

        new TestContext(eclReturn, Some(periodKey)) {
          implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
            ReturnDataRequest(
              fakeRequest,
              eclReturn.internalId,
              eclReturn,
              None,
              eclRegistrationReference,
              Some(periodKey)
            )

          when(mockEclReturnsService.getEclReturnSubmission(any(), any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetEclReturnSubmissionResponse](
                Future.successful(Right(eclReturnSubmission))
              )
            )

          when(mockRegistrationService.getSubscription(any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetSubscriptionResponse](
                Future.successful(Right(subscriptionResponse))
              )
            )

          when(mockEclReturnsService.upsertReturn(any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, Unit](
                Future.successful(Left(DataHandlingError.InternalUnexpectedError(None, None)))
              )
            )

          val result: Future[Result] = controller.onSubmit()(returnDataRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "return InternalServerError (500) when returnsService.submitReturn fails" in forAll {
      (
        validEclReturn: ValidEclReturn,
        validEclSubmission: ValidGetEclReturnSubmissionResponse,
        subscriptionResponse: GetSubscriptionResponse
      ) =>
        val eclReturn: EclReturn                                = validEclReturn.eclReturn
        val eclReturnSubmission: GetEclReturnSubmissionResponse =
          createTestEclReturnSubmission(validEclReturn, validEclSubmission)

        new TestContext(eclReturn, Some(periodKey)) {
          implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
            ReturnDataRequest(
              fakeRequest,
              eclReturn.internalId,
              eclReturn,
              None,
              eclRegistrationReference,
              Some(periodKey)
            )

          when(mockEclReturnsService.getEclReturnSubmission(any(), any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetEclReturnSubmissionResponse](
                Future.successful(Right(eclReturnSubmission))
              )
            )

          when(mockRegistrationService.getSubscription(any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetSubscriptionResponse](
                Future.successful(Right(subscriptionResponse))
              )
            )

          when(mockEclReturnsService.upsertReturn(any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, Unit](Future.successful(Right(())))
            )

          when(mockEclReturnsService.submitReturn(ArgumentMatchers.eq(eclReturn.internalId))(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, SubmitEclReturnResponse](
                Future.successful(Left(DataHandlingError.InternalUnexpectedError(None, None)))
              )
            )

          val result: Future[Result] = controller.onSubmit()(returnDataRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }

    "return InternalServerError (500) when return type is not set" in forAll {
      (
        validEclReturn: ValidEclReturn,
        validEclSubmission: ValidGetEclReturnSubmissionResponse,
        subscriptionResponse: GetSubscriptionResponse
      ) =>
        val eclReturn: EclReturn                                = validEclReturn.eclReturn
          .copy(returnType = None)
        val eclReturnSubmission: GetEclReturnSubmissionResponse =
          createTestEclReturnSubmission(validEclReturn, validEclSubmission)

        new TestContext(eclReturn, Some(periodKey)) {
          implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
            ReturnDataRequest(
              fakeRequest,
              eclReturn.internalId,
              eclReturn,
              None,
              eclRegistrationReference,
              Some(periodKey)
            )

          when(mockRegistrationService.getSubscription(any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetSubscriptionResponse](
                Future.successful(Right(subscriptionResponse))
              )
            )

          when(mockEclReturnsService.getEclReturnSubmission(any(), any())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetEclReturnSubmissionResponse](
                Future.successful(Right(eclReturnSubmission))
              )
            )

          val result: Future[Result] = controller.onSubmit()(returnDataRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
    }
  }
}
