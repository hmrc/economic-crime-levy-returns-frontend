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
import org.mockito.ArgumentMatchers.any
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyreturns.models.{ObligationDetails, SessionKeys}
import uk.gov.hmrc.economiccrimelevyreturns.services.{ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{NilReturnSubmittedView, ReturnSubmittedView}

import scala.concurrent.Future
import scala.concurrent.Future.unit

class ReturnSubmittedControllerSpec extends SpecBase {

  val returnSubmittedView: ReturnSubmittedView       = app.injector.instanceOf[ReturnSubmittedView]
  val nilReturnSubmittedView: NilReturnSubmittedView = app.injector.instanceOf[NilReturnSubmittedView]
  val mockReturnsService: ReturnsService             = mock[ReturnsService]
  val mockSessionService: SessionService             = mock[SessionService]

  class TestContext() {
    val controller = new ReturnSubmittedController(
      mcc,
      fakeAuthorisedAction(internalId),
      returnSubmittedView,
      nilReturnSubmittedView,
      mockReturnsService,
      mockSessionService
    )
  }

  "onPageLoad" should {
    "return OK and the correct view for a submitted return" in forAll {
      (
        chargeReference: String,
        amountDue: BigDecimal,
        obligationDetails: ObligationDetails,
        email: String
      ) =>
        new TestContext() {
          implicit val authRequest: AuthorisedRequest[AnyContentAsEmpty.type] =
            AuthorisedRequest(fakeRequest, internalId, eclRegistrationReference)
          implicit val messages: Messages                                     = messagesApi.preferred(authRequest)

          when(mockReturnsService.deleteReturn(any())(any()))
            .thenReturn(EitherT.right(unit))
          when(mockSessionService.delete(any())(any()))
            .thenReturn(EitherT.right(unit))

          val result: Future[Result] =
            controller.onPageLoad()(
              fakeRequest.withSession(
                (SessionKeys.ChargeReference, chargeReference),
                (SessionKeys.Email, email),
                (SessionKeys.ObligationDetails, Json.toJson(obligationDetails).toString()),
                (SessionKeys.AmountDue, amountDue.toString())
              )
            )

          status(result) shouldBe OK

          contentAsString(result) shouldBe returnSubmittedView(
            chargeReference,
            ViewUtils.formatToday()(messages),
            ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceDueDate)(messages),
            obligationDetails.inboundCorrespondenceFromDate.getYear.toString,
            obligationDetails.inboundCorrespondenceToDate.getYear.toString,
            amountDue,
            email
          )(fakeRequest, messages).toString()
        }
    }

    "return OK and the correct view for a submitted nil return" in forAll {
      (obligationDetails: ObligationDetails, amountDue: BigDecimal, email: String) =>
        new TestContext() {
          implicit val authRequest: AuthorisedRequest[AnyContentAsEmpty.type] =
            AuthorisedRequest(fakeRequest, internalId, eclRegistrationReference)
          implicit val messages: Messages                                     = messagesApi.preferred(authRequest)

          when(mockReturnsService.deleteReturn(any())(any()))
            .thenReturn(EitherT.right(unit))
          when(mockSessionService.delete(any())(any()))
            .thenReturn(EitherT.right(unit))

          val result: Future[Result] =
            controller.onPageLoad()(
              fakeRequest.withSession(
                (SessionKeys.Email, email),
                (SessionKeys.ObligationDetails, Json.toJson(obligationDetails).toString()),
                (SessionKeys.AmountDue, amountDue.toString())
              )
            )

          status(result) shouldBe OK

          contentAsString(result) shouldBe nilReturnSubmittedView(
            ViewUtils.formatToday(),
            obligationDetails.inboundCorrespondenceFromDate.getYear.toString,
            obligationDetails.inboundCorrespondenceToDate.getYear.toString,
            amountDue,
            email
          )(authRequest, messages).toString()
        }
    }
  }

}
