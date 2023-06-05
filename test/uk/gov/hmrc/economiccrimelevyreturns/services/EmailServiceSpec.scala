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
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.email.ReturnSubmittedEmailParameters
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn, ObligationDetails}
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils

import scala.concurrent.Future

class EmailServiceSpec extends SpecBase {

  val mockEmailConnector: EmailConnector = mock[EmailConnector]
  val service                            = new EmailService(mockEmailConnector, appConfig)

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
          amountDue,
          "true"
        )

        when(
          mockEmailConnector
            .sendReturnSubmittedEmail(
              ArgumentMatchers.eq(validEclReturn.eclReturn.contactEmailAddress.get),
              ArgumentMatchers.eq(expectedParams)
            )(any())
        )
          .thenReturn(Future.successful(()))

        val result: Unit =
          await(service.sendReturnSubmittedEmail(validEclReturn.eclReturn, chargeReference)(hc, messages))

        result shouldBe ()

        verify(mockEmailConnector, times(1))
          .sendReturnSubmittedEmail(any(), any())(any())

        reset(mockEmailConnector)
    }

    "throw an IllegalStateException when there are no obligation details in the return data" in forAll {
      (internalId: String, chargeReference: Option[String]) =>
        val result = intercept[IllegalStateException] {
          await(service.sendReturnSubmittedEmail(EclReturn.empty(internalId), chargeReference)(hc, messages))
        }

        result.getMessage shouldBe "No obligation details found in return data"
    }

    "throw an IllegalStateException when the contact details are missing" in forAll {
      (
        internalId: String,
        chargeReference: Option[String],
        obligationDetails: ObligationDetails,
        calculatedLiability: CalculatedLiability
      ) =>
        val result = intercept[IllegalStateException] {
          await(
            service.sendReturnSubmittedEmail(
              EclReturn
                .empty(internalId)
                .copy(obligationDetails = Some(obligationDetails), calculatedLiability = Some(calculatedLiability)),
              chargeReference
            )(hc, messages)
          )
        }

        result.getMessage shouldBe "Invalid contact details"
    }
  }

}
