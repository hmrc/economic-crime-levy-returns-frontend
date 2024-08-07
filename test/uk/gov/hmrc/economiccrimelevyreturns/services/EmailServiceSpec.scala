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

package uk.gov.hmrc.economiccrimelevyreturns.services

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyreturns.ValidEclReturn
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EmailConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.email.{AmendReturnSubmittedParameters, ReturnSubmittedEmailParameters}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.EmailSubmissionError
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, GetCorrespondenceAddressDetails}
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils

import scala.concurrent.Future

class EmailServiceSpec extends SpecBase {

  val mockEmailConnector: EmailConnector = mock[EmailConnector]
  val service                            = new EmailService(mockEmailConnector)

  def clearContactInfo(eclReturn: EclReturn): EclReturn =
    eclReturn.copy(
      contactName = None,
      contactRole = None,
      contactEmailAddress = None,
      contactTelephoneNumber = None
    )

  "sendReturnSubmittedEmail" should {
    "send an email to the contact in the return" in forAll {
      (validEclReturn: ValidEclReturn, chargeReference: Option[String]) =>
        val obligationDetails   = validEclReturn.eclReturn.obligationDetails.get
        val calculatedLiability = validEclReturn.eclReturn.calculatedLiability.get

        val eclDueDate      =
          ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceDueDate, translate = false)(messages)
        val dateSubmitted   = ViewUtils.formatToday(translate = false)(messages)
        val periodStartDate =
          ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceFromDate, translate = false)(messages)
        val periodEndDate   =
          ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceToDate, translate = false)(messages)
        val amountDue       = ViewUtils.formatMoney(calculatedLiability.amountDue.amount)

        val expectedParams = ReturnSubmittedEmailParameters(
          name = validEclReturn.eclReturn.contactName.get,
          dateSubmitted = dateSubmitted,
          periodStartDate = periodStartDate,
          periodEndDate = periodEndDate,
          chargeReference = chargeReference,
          fyStartYear = obligationDetails.inboundCorrespondenceFromDate.getYear.toString,
          fyEndYear = obligationDetails.inboundCorrespondenceToDate.getYear.toString,
          datePaymentDue = if (chargeReference.isDefined) Some(eclDueDate) else None,
          amountDue
        )

        when(
          mockEmailConnector
            .sendReturnSubmittedEmail(
              ArgumentMatchers.eq(validEclReturn.eclReturn.contactEmailAddress.get),
              ArgumentMatchers.eq(expectedParams)
            )(any())
        )
          .thenReturn(Future.successful(()))

        val result =
          await(service.sendReturnSubmittedEmail(validEclReturn.eclReturn, chargeReference)(hc, messages).value)

        result shouldBe Right(())

        verify(mockEmailConnector, times(1))
          .sendReturnSubmittedEmail(any(), any())(any())

        reset(mockEmailConnector)
    }

    "return an internal unexpected error when unable to send email due to insufficient input data" in forAll {
      (internalId: String, chargeReference: Option[String]) =>
        val result = await(
          service.sendReturnSubmittedEmail(EclReturn.empty(internalId, None), chargeReference)(hc, messages).value
        )

        result shouldBe Left(
          EmailSubmissionError
            .InternalUnexpectedError(None, Some("Missing required input data for sending return email."))
        )
    }
  }

  "sendAmendReturnConfirmationEmail" should {
    "send an amendment successful email to the contact in the return" in forAll { (validEclReturn: ValidEclReturn) =>
      val obligationDetails = validEclReturn.eclReturn.obligationDetails.get
      val dateSubmitted     = ViewUtils.formatToday(translate = false)(messages)
      val periodStartDate   =
        ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceFromDate, translate = false)(messages)
      val periodEndDate     =
        ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceToDate, translate = false)(messages)

      val validAddress = GetCorrespondenceAddressDetails("Test address", None, None, None, None, None)

      val expectedParams = AmendReturnSubmittedParameters(
        name = validEclReturn.eclReturn.contactName.get,
        dateSubmitted = dateSubmitted,
        periodStartDate = periodStartDate,
        periodEndDate = periodEndDate,
        addressLine1 = Some("Test address"),
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        containsAddress = Some("true")
      )

      when(
        mockEmailConnector
          .sendAmendReturnSubmittedEmail(
            ArgumentMatchers.eq(validEclReturn.eclReturn.contactEmailAddress.get),
            ArgumentMatchers.eq(expectedParams)
          )(any())
      )
        .thenReturn(Future.successful(()))

      val result =
        await(
          service.sendAmendReturnConfirmationEmail(validEclReturn.eclReturn, Some(validAddress))(hc, messages).value
        )

      result shouldBe Right(())

      verify(mockEmailConnector, times(1))
        .sendAmendReturnSubmittedEmail(any(), any())(any())

      reset(mockEmailConnector)
    }

    "return an error if insufficient data" in forAll { (validEclReturn: ValidEclReturn) =>
      val validAddress = GetCorrespondenceAddressDetails("Test address", None, None, None, None, None)

      val result = await(
        service
          .sendAmendReturnConfirmationEmail(clearContactInfo(validEclReturn.eclReturn), Some(validAddress))(
            hc,
            messages
          )
          .value
      )

      result shouldBe
        Left(
          EmailSubmissionError
            .InternalUnexpectedError(None, Some("Missing required input data for amend return email."))
        )

    }
  }
}
