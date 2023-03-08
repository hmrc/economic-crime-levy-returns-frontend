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
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.forms.ContactRoleFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.navigation.ContactRolePageNavigator
import uk.gov.hmrc.economiccrimelevyreturns.views.html.ContactRoleView

import scala.concurrent.Future

class ContactRoleControllerSpec extends SpecBase {

  val view: ContactRoleView                 = app.injector.instanceOf[ContactRoleView]
  val formProvider: ContactRoleFormProvider = new ContactRoleFormProvider()
  val form: Form[String]                    = formProvider()

  val pageNavigator: ContactRolePageNavigator = new ContactRolePageNavigator() {
    override protected def navigateInNormalMode(eclReturn: EclReturn): Call = onwardRoute
  }

  val mockEclReturnsConnector: EclReturnsConnector = mock[EclReturnsConnector]

  val roleMaxLength: Int = 160

  class TestContext(returnData: EclReturn) {
    val controller = new ContactRoleController(
      mcc,
      fakeAuthorisedAction(returnData.internalId),
      fakeDataRetrievalAction(returnData),
      mockEclReturnsConnector,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (eclReturn: EclReturn, name: String) =>
        new TestContext(eclReturn.copy(contactName = Some(name), contactRole = None)) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, name, NormalMode)(fakeRequest, messages).toString
        }
    }

    "throw an IllegalStateException when there is no contact name in the returns data" in forAll {
      (eclReturn: EclReturn) =>
        val updatedReturn = eclReturn.copy(contactName = None)

        new TestContext(updatedReturn) {
          val result: IllegalStateException = intercept[IllegalStateException] {
            await(controller.onPageLoad(NormalMode)(fakeRequest))
          }

          result.getMessage shouldBe "No contact name found in returns data"
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (eclReturn: EclReturn, role: String, name: String) =>
        new TestContext(eclReturn.copy(contactName = Some(name), contactRole = Some(role))) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(role), name, NormalMode)(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided contact role then redirect to the next page" in forAll(
      Arbitrary.arbitrary[EclReturn],
      stringsWithMaxLength(roleMaxLength)
    ) { (eclReturn: EclReturn, role: String) =>
      new TestContext(eclReturn) {
        val updatedReturn: EclReturn = eclReturn.copy(contactRole = Some(role))

        when(mockEclReturnsConnector.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(Future.successful(updatedReturn))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", role)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (eclReturn: EclReturn, name: String) =>
        new TestContext(eclReturn.copy(contactName = Some(name))) {
          val result: Future[Result]       =
            controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[String] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(formWithErrors, name, NormalMode)(fakeRequest, messages).toString
        }
    }
  }
}
