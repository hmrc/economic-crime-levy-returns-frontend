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

import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyreturns.models.{ObligationDetails, Open, SessionKeys}
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyreturns.views.html.ReturnSubmittedView

import java.time.LocalDate
import scala.concurrent.Future

class ReturnSubmittedControllerSpec extends SpecBase {

  val view: ReturnSubmittedView = app.injector.instanceOf[ReturnSubmittedView]

  val controller = new ReturnSubmittedController(
    mcc,
    fakeAuthorisedAction(internalId),
    view
  )

  "onPageLoad" should {
    "return OK and the correct view" in forAll {
      (chargeReference: String, obligationDetails: ObligationDetails, amountDue: BigDecimal) =>
        implicit val authRequest: AuthorisedRequest[AnyContentAsEmpty.type] =
          AuthorisedRequest(fakeRequest, internalId, eclRegistrationReference)
        implicit val messages: Messages                                     = messagesApi.preferred(authRequest)

        val result: Future[Result] =
          controller.onPageLoad()(
            fakeRequest.withSession(
              (SessionKeys.ChargeReference, chargeReference),
              (SessionKeys.ObligationDetails, Json.toJson(obligationDetails).toString()),
              (SessionKeys.AmountDue, amountDue.toString())
            )
          )

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(
          chargeReference,
          ViewUtils.formatToday(),
          ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceDueDate),
          obligationDetails.inboundCorrespondenceFromDate.getYear.toString,
          obligationDetails.inboundCorrespondenceToDate.getYear.toString,
          amountDue
        )(authRequest, messages).toString()
    }

    "throw an IllegalStateException when the charge reference is not found in the session" in {
      val result: IllegalStateException = intercept[IllegalStateException] {
        await(controller.onPageLoad()(fakeRequest))
      }

      result.getMessage shouldBe "Charge reference number not found in session"
    }

    "throw an IllegalStateException when the amount due is not found in session" in {
      val obligationDetails             = ObligationDetails(
        status = Open,
        inboundCorrespondenceFromDate = LocalDate.now(),
        inboundCorrespondenceToDate = LocalDate.now(),
        inboundCorrespondenceDateReceived = None,
        inboundCorrespondenceDueDate = LocalDate.now(),
        periodKey = "22XY"
      )
      val result: IllegalStateException = intercept[IllegalStateException] {
        await(
          controller.onPageLoad()(
            fakeRequest.withSession(
              (SessionKeys.ChargeReference, "test-charge-reference"),
              (SessionKeys.ObligationDetails, Json.toJson(obligationDetails).toString())
            )
          )
        )
      }

      result.getMessage shouldBe "Amount due not found in session"
    }

    "throw an IllegalStateException when the obligation details are not found in the session" in {
      val result: IllegalStateException = intercept[IllegalStateException] {
        await(controller.onPageLoad()(fakeRequest.withSession((SessionKeys.ChargeReference, "test-charge-reference"))))
      }

      result.getMessage shouldBe "Obligation details not found in session"
    }
  }

}
