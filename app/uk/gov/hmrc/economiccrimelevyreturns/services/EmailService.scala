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

import play.api.i18n.Messages
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EmailConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.email.ReturnSubmittedEmailParameters
import uk.gov.hmrc.economiccrimelevyreturns.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailService @Inject() (emailConnector: EmailConnector)(implicit
  ec: ExecutionContext
) {

  def sendReturnSubmittedEmail(eclReturn: EclReturn, chargeReference: String)(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): Future[Unit] = {
    val eclDueDate      = ViewUtils.formatLocalDate(EclTaxYear.dueDate, translate = false)
    val dateSubmitted   = ViewUtils.formatToday(translate = false)
    val periodStartDate = ViewUtils.formatLocalDate(EclTaxYear.currentFinancialYearStartDate, translate = false)
    val periodEndDate   = ViewUtils.formatLocalDate(EclTaxYear.currentFinancialYearEndDate, translate = false)

    (
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
            fyStartYear = EclTaxYear.currentFyStartYear,
            fyEndYear = EclTaxYear.currentFyEndYear,
            datePaymentDue = eclDueDate
          )
        )
      case _                                => throw new IllegalStateException("Invalid contact details")
    }
  }
}
