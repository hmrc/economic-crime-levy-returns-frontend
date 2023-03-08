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
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionKeys
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyreturns.views.html.ReturnSubmittedView

import java.time.Instant
import scala.concurrent.Future

class ReturnSubmittedControllerSpec extends SpecBase {

  val view: ReturnSubmittedView = app.injector.instanceOf[ReturnSubmittedView]

  val controller = new ReturnSubmittedController(
    mcc,
    fakeAuthorisedAction(internalId),
    view
  )

  "onPageLoad" should {
    "return OK and the correct view" in forAll { (eclReference: String, submittedWhen: Instant) =>
      implicit val authRequest: AuthorisedRequest[AnyContentAsEmpty.type] =
        AuthorisedRequest(fakeRequest, internalId, eclRegistrationReference)
      implicit val messages: Messages                                     = messagesApi.preferred(authRequest)

      val submittedWhenText = ViewUtils.formatInstantAsLocalDate(submittedWhen)

      val result: Future[Result] =
        controller.onPageLoad()(
          fakeRequest.withSession(
            (SessionKeys.EclReference, eclReference),
            (SessionKeys.SubmittedWhen, submittedWhenText)
          )
        )

      status(result) shouldBe OK

      contentAsString(result) shouldBe view(eclReference, submittedWhenText)(authRequest, messages).toString
    }

    "throw an IllegalStateException when the ECL reference is not found in the session" in forAll {
      submittedWhenText: String =>
        val result: IllegalStateException = intercept[IllegalStateException] {
          await(controller.onPageLoad()(fakeRequest.withSession((SessionKeys.SubmittedWhen, submittedWhenText))))
        }

        result.getMessage shouldBe "ECL reference number not found in session"
    }

    "throw an IllegalStateException when the submission date is not found in the session" in forAll {
      eclReference: String =>
        val result: IllegalStateException = intercept[IllegalStateException] {
          await(controller.onPageLoad()(fakeRequest.withSession((SessionKeys.EclReference, eclReference))))
        }

        result.getMessage shouldBe "ECL return submission date not found in session"
    }
  }

}
