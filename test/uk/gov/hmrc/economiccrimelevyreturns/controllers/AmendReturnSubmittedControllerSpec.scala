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
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.data.Form
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.forms.AmendReasonFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataHandlingError
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, Band, EclReturn, GetSubscriptionResponse, ObligationDetails, SessionKeys}
import uk.gov.hmrc.economiccrimelevyreturns.services.{RegistrationService, ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.AmendReturnSubmittedView

import scala.concurrent.Future
import scala.concurrent.Future.unit

class AmendReturnSubmittedControllerSpec extends SpecBase {

  val view: AmendReturnSubmittedView                  = app.injector.instanceOf[AmendReturnSubmittedView]
  val formProvider: AmendReasonFormProvider           = new AmendReasonFormProvider()
  val form: Form[String]                              = formProvider()
  val mockEclRegistrationService: RegistrationService = mock[RegistrationService]
  val mockReturnsService: ReturnsService              = mock[ReturnsService]
  val mockSessionService: SessionService              = mock[SessionService]

  class TestContext(returnsData: EclReturn) {
    val controller = new AmendReturnSubmittedController(
      appConfig,
      mcc,
      fakeAuthorisedAction(returnsData.internalId),
      mockEclRegistrationService,
      view,
      mockSessionService,
      mockReturnsService
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll {
      (
        eclReturn: EclReturn,
        subscription: GetSubscriptionResponse,
        obligationDetails: ObligationDetails,
        email: String
      ) =>
        val band       = random[Band]
        val amountDue  = random[Int]
        val isIncrease = random[Boolean]
        new TestContext(
          eclReturn.copy(
            contactEmailAddress = Some(email),
            obligationDetails = Some(obligationDetails),
            returnType = Some(AmendReturn)
          )
        ) {
          when(mockReturnsService.deleteReturn(anyString())(any()))
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))
          when(mockSessionService.delete(anyString())(any()))
            .thenReturn(EitherT.right(unit))
          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, GetSubscriptionResponse](Future.successful(Right(subscription)))
            )

          val result: Future[Result] = controller.onPageLoad()(
            fakeRequest.withSession(
              (SessionKeys.email             -> email),
              (SessionKeys.band              -> band.toString),
              (SessionKeys.amountDue         -> amountDue.toString),
              (SessionKeys.isIncrease        -> isIncrease.toString),
              (SessionKeys.obligationDetails -> Json.stringify(Json.toJson(obligationDetails)))
            )
          )

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            obligationDetails.inboundCorrespondenceFromDate,
            obligationDetails.inboundCorrespondenceToDate,
            email,
            Some(subscription.correspondenceAddressDetails),
            "test-ecl-registration-reference",
            band.toString,
            amountDue,
            isIncrease
          )(fakeRequest, messages).toString
        }
    }
  }

  "return Internal server error when email is missing from the session" in forAll {
    (
      eclReturn: EclReturn,
      obligationDetails: ObligationDetails
    ) =>
      new TestContext(
        eclReturn.copy(
          obligationDetails = Some(obligationDetails)
        )
      ) {
        when(mockReturnsService.deleteReturn(anyString())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))
        when(mockSessionService.delete(anyString())(any()))
          .thenReturn(EitherT.right(unit))

        val result: Future[Result] = controller.onPageLoad()(
          fakeRequest.withSession(
            SessionKeys.obligationDetails -> Json.stringify(Json.toJson(obligationDetails))
          )
        )

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
  }

  "return Internal server error when obligation details is missing from the session" in forAll {
    (
      eclReturn: EclReturn,
      email: String
    ) =>
      new TestContext(
        eclReturn.copy(
          contactEmailAddress = Some(email)
        )
      ) {
        when(mockReturnsService.deleteReturn(anyString())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))
        when(mockSessionService.delete(anyString())(any()))
          .thenReturn(EitherT.right(unit))

        val result: Future[Result] = controller.onPageLoad()(
          fakeRequest.withSession(
            SessionKeys.email -> email
          )
        )

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
  }
}
