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
import org.scalacheck.{Arbitrary, Gen}
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, RequestHeader, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.cleanup.AmlRegulatedActivityLengthDataCleanup
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.forms.AmlRegulatedActivityLengthFormProvider
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, Mode}
import uk.gov.hmrc.economiccrimelevyreturns.navigation.AmlRegulatedActivityLengthPageNavigator
import uk.gov.hmrc.economiccrimelevyreturns.services.EclLiabilityService
import uk.gov.hmrc.economiccrimelevyreturns.views.html.AmlRegulatedActivityLengthView

import scala.concurrent.Future

class AmlRegulatedActivityLengthControllerSpec extends SpecBase {

  val view: AmlRegulatedActivityLengthView                 = app.injector.instanceOf[AmlRegulatedActivityLengthView]
  val formProvider: AmlRegulatedActivityLengthFormProvider = new AmlRegulatedActivityLengthFormProvider()
  val form: Form[Int]                                      = formProvider()

  val mockEclReturnsConnector: EclReturnsConnector = mock[EclReturnsConnector]
  val mockEclLiabilityService: EclLiabilityService = mock[EclLiabilityService]

  val pageNavigator: AmlRegulatedActivityLengthPageNavigator = new AmlRegulatedActivityLengthPageNavigator(
    mockEclLiabilityService
  ) {
    override protected def navigateInNormalMode(eclReturn: EclReturn)(implicit request: RequestHeader): Future[Call] =
      Future.successful(onwardRoute)

    override protected def navigateInCheckMode(eclReturn: EclReturn)(implicit request: RequestHeader): Future[Call] =
      Future.successful(onwardRoute)
  }

  val dataCleanup: AmlRegulatedActivityLengthDataCleanup = new AmlRegulatedActivityLengthDataCleanup {
    override def cleanup(eclReturn: EclReturn): EclReturn = eclReturn
  }

  val minDays = 1
  val maxDays = 999

  class TestContext(eclReturnData: EclReturn) {
    val controller = new AmlRegulatedActivityLengthController(
      mcc,
      fakeAuthorisedAction,
      fakeDataRetrievalAction(eclReturnData),
      mockEclReturnsConnector,
      formProvider,
      pageNavigator,
      dataCleanup,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (eclReturn: EclReturn, mode: Mode) =>
        new TestContext(eclReturn.copy(amlRegulatedActivityLength = None)) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, mode)(fakeRequest, messages).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (eclReturn: EclReturn, amlRegulatedActivityLength: Int, mode: Mode) =>
        new TestContext(
          eclReturn.copy(amlRegulatedActivityLength = Some(amlRegulatedActivityLength))
        ) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(amlRegulatedActivityLength), mode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided AML regulated activity length then redirect to the next page" in forAll(
      Arbitrary.arbitrary[EclReturn],
      Gen.chooseNum[Int](minDays, maxDays),
      Arbitrary.arbitrary[Mode]
    ) { (eclReturn: EclReturn, amlRegulatedActivityLength: Int, mode: Mode) =>
      new TestContext(eclReturn) {
        val updatedReturn: EclReturn =
          eclReturn.copy(amlRegulatedActivityLength = Some(amlRegulatedActivityLength))

        when(mockEclReturnsConnector.upsertReturn(ArgumentMatchers.eq(updatedReturn))(any()))
          .thenReturn(Future.successful(updatedReturn))

        val result: Future[Result] =
          controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", amlRegulatedActivityLength.toString)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll(
      Arbitrary.arbitrary[EclReturn],
      Gen.alphaStr,
      Arbitrary.arbitrary[Mode]
    ) { (eclReturn: EclReturn, invalidLength: String, mode: Mode) =>
      new TestContext(eclReturn) {
        val result: Future[Result]    =
          controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", invalidLength)))
        val formWithErrors: Form[Int] = form.bind(Map("value" -> invalidLength))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, mode)(fakeRequest, messages).toString
      }
    }
  }
}
