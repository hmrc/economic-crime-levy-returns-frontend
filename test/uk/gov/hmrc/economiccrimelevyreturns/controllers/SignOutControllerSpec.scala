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

import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.views.html.SignedOutView

import java.net.URLEncoder

class SignOutControllerSpec extends SpecBase {

  val view: SignedOutView = app.injector.instanceOf[SignedOutView]

  val controller = new SignOutController(
    mcc,
    appConfig,
    view
  )

  "signOut" should {
    "redirect to sign out, specifying the exit survey as the continue URL" in {
      val result = controller.signOut()(fakeRequest)

      val encodedContinueUrl  = URLEncoder.encode(appConfig.exitSurveyUrl, "UTF-8")
      val expectedRedirectUrl = s"${appConfig.signOutUrl}?continue=$encodedContinueUrl"

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe expectedRedirectUrl
    }
  }

  "signOutNoSurvey" should {
    "redirect to sign out, specifying SignedOut as the continue URL" in {
      val result = controller.signOutNoSurvey()(fakeRequest)

      val encodedContinueUrl  = URLEncoder.encode(routes.SignOutController.signedOut().url, "UTF-8")
      val expectedRedirectUrl = s"${appConfig.signOutUrl}?continue=$encodedContinueUrl"

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe expectedRedirectUrl
    }
  }

  "signedOut" should {
    "return OK and the correct view" in {
      val result = controller.signedOut()(fakeRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe view()(fakeRequest, messages).toString
    }
  }

}
