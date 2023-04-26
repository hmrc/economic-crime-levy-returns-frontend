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

import play.api.Logging
import play.api.i18n.Messages
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EmailConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.email.ReturnSubmittedEmailParameters
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailService @Inject() (emailConnector: EmailConnector)(implicit ec: ExecutionContext) extends Logging {

  def sendReturnSubmittedEmail(eclReturn: EclReturn, chargeReference: String)(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): Future[Unit] = {
    val obligationDetails   = eclReturn.obligationDetails.getOrElse(
      throw new IllegalStateException("No obligation details found in return data")
    )
    val calculatedLiability = eclReturn.calculatedLiability.getOrElse(
      throw new IllegalStateException("No calculated liability details found in return data")
    )

    val eclDueDate      = ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceDueDate, translate = false)
    val dateSubmitted   = ViewUtils.formatToday(translate = false)
    val periodStartDate = ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceFromDate, translate = false)
    val periodEndDate   = ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceToDate, translate = false)
    val fyStartYear     = obligationDetails.inboundCorrespondenceFromDate.getYear.toString
    val fyEndYear       = obligationDetails.inboundCorrespondenceToDate.getYear.toString
    val amountDue       = ViewUtils.formatMoney(calculatedLiability.amountDue.amount)

    ((
      eclReturn.contactName,
      eclReturn.contactEmailAddress
    ) match {
      case (Some(name), Some(emailAddress)) =>
        emailConnector.sendReturnSubmittedEmail(
          emailAddress,
          ReturnSubmittedEmailParameters(
            name = name,
            dateSubmitted = dateSubmitted,
            periodStartDate = periodStartDate,
            periodEndDate = periodEndDate,
            chargeReference = chargeReference,
            fyStartYear = fyStartYear,
            fyEndYear = fyEndYear,
            datePaymentDue = eclDueDate,
            amountDue = amountDue
          )
        )
      case _                                => throw new IllegalStateException("Invalid contact details")
    }).recover { case e: Throwable =>
      logger.error(s"Failed to send email: ${e.getMessage}")
      throw e
    }
  }
}
