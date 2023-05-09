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
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.ValidEclReturn
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.FakeValidatedReturnAction
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, SessionKeys, SubmitEclReturnResponse}
import uk.gov.hmrc.economiccrimelevyreturns.services.EmailService
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers._
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyreturns.views.html.CheckYourAnswersView
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  val view: CheckYourAnswersView = app.injector.instanceOf[CheckYourAnswersView]

  val mockEclReturnsConnector: EclReturnsConnector = mock[EclReturnsConnector]
  val mockEmailService: EmailService               = mock[EmailService]

  class TestContext(eclReturnData: EclReturn) {
    val controller = new CheckYourAnswersController(
      messagesApi,
      fakeAuthorisedAction(eclReturnData.internalId),
      fakeDataRetrievalAction(eclReturnData),
      new FakeValidatedReturnAction(eclReturnData),
      mockEclReturnsConnector,
      mockEmailService,
      mcc,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll { validEclReturn: ValidEclReturn =>
      new TestContext(validEclReturn.eclReturn) {
        implicit val returnDataRequest: ReturnDataRequest[AnyContentAsEmpty.type] =
          ReturnDataRequest(
            fakeRequest,
            validEclReturn.eclReturn.internalId,
            validEclReturn.eclReturn,
            eclRegistrationReference
          )

        implicit val messages: Messages = messagesApi.preferred(returnDataRequest)

        val result: Future[Result] = controller.onPageLoad()(returnDataRequest)

        val eclDetails: SummaryList = SummaryListViewModel(
          rows = Seq(
            EclReferenceNumberSummary.row(),
            RelevantAp12MonthsSummary.row(),
            RelevantApLengthSummary.row(),
            UkRevenueSummary.row(),
            AmlRegulatedActivitySummary.row(),
            AmlRegulatedActivityLengthSummary.row(),
            CalculatedBandSummary.row(),
            AmountDueSummary.row()
          ).flatten
        ).withCssClass("govuk-!-margin-bottom-9")

        val contactDetails: SummaryList = SummaryListViewModel(
          rows = Seq(
            ContactNameSummary.row(),
            ContactRoleSummary.row(),
            ContactEmailSummary.row(),
            ContactNumberSummary.row()
          ).flatten
        ).withCssClass("govuk-!-margin-bottom-9")

        status(result)          shouldBe OK
        contentAsString(result) shouldBe view(eclDetails, contactDetails)(fakeRequest, messages).toString
      }
    }
  }

  "onSubmit" should {
    "redirect to the ECL return submitted page after submitting the ECL return successfully" in forAll {
      (submitEclReturnResponse: SubmitEclReturnResponse, validEclReturn: ValidEclReturn) =>
        new TestContext(validEclReturn.eclReturn) {
          when(mockEclReturnsConnector.upsertReturn(any())(any()))
            .thenReturn(Future.successful(validEclReturn.eclReturn))

          when(mockEclReturnsConnector.submitReturn(ArgumentMatchers.eq(validEclReturn.eclReturn.internalId))(any()))
            .thenReturn(Future.successful(submitEclReturnResponse))

          when(mockEclReturnsConnector.deleteReturn(ArgumentMatchers.eq(validEclReturn.eclReturn.internalId))(any()))
            .thenReturn(Future.successful(()))

          val result: Future[Result] = controller.onSubmit()(fakeRequest)

          status(result)                                     shouldBe SEE_OTHER
          session(result).get(SessionKeys.ChargeReference)   shouldBe Some(submitEclReturnResponse.chargeReference)
          session(result).get(SessionKeys.ObligationDetails) shouldBe Some(
            Json.toJson(validEclReturn.eclReturn.obligationDetails.get).toString()
          )
          session(result).get(SessionKeys.AmountDue)         shouldBe Some(
            validEclReturn.eclReturn.calculatedLiability.get.amountDue.amount.toString()
          )
          redirectLocation(result)                           shouldBe Some(routes.ReturnSubmittedController.onPageLoad().url)

          verify(mockEmailService, times(1)).sendReturnSubmittedEmail(
            ArgumentMatchers.eq(validEclReturn.eclReturn),
            ArgumentMatchers.eq(submitEclReturnResponse.chargeReference)
          )(any(), any())

          reset(mockEmailService)
        }
    }

    "throw an IllegalStateException when the calculated liability is not present in the ECL return" in forAll {
      (submitEclReturnResponse: SubmitEclReturnResponse, validEclReturn: ValidEclReturn) =>
        val updatedReturn = validEclReturn.eclReturn.copy(calculatedLiability = None)

        new TestContext(updatedReturn) {
          when(mockEclReturnsConnector.submitReturn(ArgumentMatchers.eq(updatedReturn.internalId))(any()))
            .thenReturn(Future.successful(submitEclReturnResponse))

          when(mockEclReturnsConnector.deleteReturn(ArgumentMatchers.eq(updatedReturn.internalId))(any()))
            .thenReturn(Future.successful(()))

          val result: IllegalStateException = intercept[IllegalStateException] {
            await(controller.onSubmit()(fakeRequest))
          }

          result.getMessage shouldBe "Amount due not found in return data"

          verify(mockEmailService, times(1)).sendReturnSubmittedEmail(
            ArgumentMatchers.eq(updatedReturn),
            ArgumentMatchers.eq(submitEclReturnResponse.chargeReference)
          )(any(), any())

          reset(mockEmailService)
        }
    }
  }

}
