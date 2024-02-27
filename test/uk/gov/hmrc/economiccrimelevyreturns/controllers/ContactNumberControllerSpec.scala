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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.forms.ContactNumberFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.{MinMaxValues, Regex}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataHandlingError
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.navigation.ContactNumberPageNavigator
import uk.gov.hmrc.economiccrimelevyreturns.services.ReturnsService
import uk.gov.hmrc.economiccrimelevyreturns.views.html.ContactNumberView

import scala.concurrent.Future

class ContactNumberControllerSpec extends SpecBase {

  val view: ContactNumberView                 = app.injector.instanceOf[ContactNumberView]
  val formProvider: ContactNumberFormProvider = new ContactNumberFormProvider()
  val form: Form[String]                      = formProvider()

  val pageNavigator: ContactNumberPageNavigator = new ContactNumberPageNavigator() {
    override protected def navigateInNormalMode(eclReturn: EclReturn): Call = onwardRoute
  }

  val mockEclReturnsService: ReturnsService = mock[ReturnsService]

  class TestContext(returnData: EclReturn) {
    val controller = new ContactNumberController(
      mcc,
      fakeAuthorisedAction(returnData.internalId),
      fakeDataRetrievalAction(returnData),
      mockEclReturnsService,
      formProvider,
      pageNavigator,
      view,
      fakeNoOpStoreUrlAction
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (eclReturn: EclReturn, name: String) =>
        new TestContext(eclReturn.copy(contactName = Some(name), contactTelephoneNumber = None)) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, name, NormalMode)(fakeRequest, messages).toString
        }
    }

    "return InternalServerError when there is no contact name in the returns data" in forAll { (eclReturn: EclReturn) =>
      val updatedReturn = eclReturn.copy(contactName = None)

      new TestContext(updatedReturn) {
        val result: Future[Result] = controller.onSubmit(NormalMode)(fakeRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (eclReturn: EclReturn, number: String, name: String) =>
        new TestContext(eclReturn.copy(contactName = Some(name), contactTelephoneNumber = Some(number))) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(number), name, NormalMode)(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided contact number then redirect to the next page" in forAll(
      Arbitrary.arbitrary[EclReturn],
      stringFromRegex(MinMaxValues.TelephoneNumberMaxLength, Regex.TelephoneNumberRegex)
    ) { (eclReturn: EclReturn, number: String) =>
      new TestContext(eclReturn) {
        val updatedReturn: EclReturn = eclReturn.copy(contactTelephoneNumber = Some(number.filterNot(_.isWhitespace)))

        when(mockEclReturnsService.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", number)))

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
