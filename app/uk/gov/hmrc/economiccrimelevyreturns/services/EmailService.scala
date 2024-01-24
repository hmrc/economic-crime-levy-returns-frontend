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

import cats.data.EitherT
import play.api.Logging
import play.api.i18n.Messages
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EmailConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.email.{AmendReturnSubmittedParameters, ReturnSubmittedEmailParameters}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.EmailSubmissionError
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailService @Inject() (emailConnector: EmailConnector)(implicit ec: ExecutionContext) extends Logging {

  def sendReturnSubmittedEmail(eclReturn: EclReturn, chargeReference: Option[String])(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): EitherT[Future, EmailSubmissionError, Unit] =
    EitherT {
      (
        eclReturn.obligationDetails,
        eclReturn.calculatedLiability,
        eclReturn.contactName,
        eclReturn.contactEmailAddress
      ) match {
        case (Some(obligationDetails), Some(calculatedLiability), Some(name), Some(emailAddress)) =>
          val eclDueDate      = ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceDueDate, translate = false)
          val dateSubmitted   = ViewUtils.formatToday(translate = false)
          val periodStartDate =
            ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceFromDate, translate = false)
          val periodEndDate   =
            ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceToDate, translate = false)
          val fyStartYear     = obligationDetails.inboundCorrespondenceFromDate.getYear.toString
          val fyEndYear       = obligationDetails.inboundCorrespondenceToDate.getYear.toString
          val amountDue       = ViewUtils.formatMoney(calculatedLiability.amountDue.amount)
          val datePaymentDue  = if (chargeReference.isDefined) Some(eclDueDate) else None

          emailConnector
            .sendReturnSubmittedEmail(
              emailAddress,
              ReturnSubmittedEmailParameters(
                name,
                dateSubmitted,
                periodStartDate,
                periodEndDate,
                chargeReference,
                fyStartYear,
                fyEndYear,
                datePaymentDue,
                amountDue
              )
            )
            .map(Right(_))
        case _                                                                                    =>
          Future.successful(
            Left(
              EmailSubmissionError
                .InternalUnexpectedError(None, Some("Missing required input data for sending return email."))
            )
          )
      }
    }

  def sendAmendReturnConfirmationEmail(eclReturn: EclReturn)(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): EitherT[Future, EmailSubmissionError, Unit] =
    EitherT {
      (eclReturn.contactName, eclReturn.contactEmailAddress, eclReturn.obligationDetails) match {
        case (Some(name), Some(emailAddress), Some(obligationDetails)) =>
          val dateSubmitted   = ViewUtils.formatToday(translate = false)
          val periodStartDate = ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceFromDate)
          val periodToDate    = ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceToDate)

          emailConnector
            .sendAmendReturnSubmittedEmail(
              emailAddress,
              AmendReturnSubmittedParameters(name, dateSubmitted, periodStartDate, periodToDate)
            )
            .map(Right(_))
        case _                                                         =>
          Future.successful(
            Left(
              EmailSubmissionError
                .InternalUnexpectedError(None, Some("Missing required input data for amend return email."))
            )
          )
      }
    }
}
