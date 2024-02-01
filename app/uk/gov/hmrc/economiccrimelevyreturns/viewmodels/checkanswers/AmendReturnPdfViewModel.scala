/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers

import play.api.http.{ContentTypeOf, ContentTypes, Writeable}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, GetEclReturnSubmissionResponse}
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.govuk.summarylist._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}

import java.time.LocalDate

case class AmendReturnPdfViewModel(
  date: LocalDate,
  eclReturn: EclReturn,
  eclReturnSubmission: Option[GetEclReturnSubmissionResponse]
) extends TrackEclReturnChanges {

  def amendedAnswersDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (
        addIf(
          hasAmlRegulatedActivityLengthChanged,
          formatRow(AmlRegulatedActivityLengthSummary.row(eclReturn.amlRegulatedActivityLength))
        ) ++
          addIf(
            hasRelevantApLengthChanged,
            formatRow(RelevantApLengthSummary.row(eclReturn.relevantApLength))
          ) ++
          addIf(
            hasUkRevenueChanged,
            formatRow(UkRevenueSummary.row(eclReturn.relevantApRevenue))
          ) ++
          addIf(
            hasCarriedOutAmlRegulatedActivityForFullFyChanged,
            formatRow(RelevantAp12MonthsSummary.row(eclReturn.carriedOutAmlRegulatedActivityForFullFy))
          ) ++
          addIf(
            hasAmlRegulatedActivityLengthChanged,
            formatRow(AmlRegulatedActivityLengthSummary.row(eclReturn.amlRegulatedActivityLength))
          ) ++
          addIf(
            hasCalculatedBandSummaryChanged,
            formatRow(CalculatedBandSummary.row(eclReturn.calculatedLiability))
          ) ++
          addIf(
            hasAmountDueSummaryChanged,
            formatRow(AmountDueSummary.row(eclReturn.calculatedLiability))
          ) ++
          addIf(
            hasContactNameChanged,
            formatRow(ContactNameSummary.row(eclReturn.contactName))
          ) ++
          addIf(
            hasContactRoleChanged,
            formatRow(ContactRoleSummary.row(eclReturn.contactRole))
          ) ++
          addIf(
            hasContactEmailAddressChanged,
            formatRow(ContactEmailSummary.row(eclReturn.contactEmailAddress))
          ) ++
          addIf(
            hasContactTelephoneNumberChanged,
            formatRow(ContactNumberSummary.row(eclReturn.contactTelephoneNumber))
          )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def amendReasonDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        formatRow(AmendReasonSummary.row(eclReturn.amendReason))
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def contactDetails()(implicit messages: Messages): SummaryList = SummaryListViewModel(
    rows = (
      addIf(
        isAmendReturnAndNot(hasContactNameChanged),
        formatRow(ContactNameSummary.row(eclReturn.contactName))
      ) ++
        addIf(
          isAmendReturnAndNot(hasContactRoleChanged),
          formatRow(ContactRoleSummary.row(eclReturn.contactRole))
        ) ++
        addIf(
          isAmendReturnAndNot(hasContactEmailAddressChanged),
          formatRow(ContactEmailSummary.row(eclReturn.contactEmailAddress))
        ) ++
        addIf(
          isAmendReturnAndNot(hasContactTelephoneNumberChanged),
          formatRow(ContactNumberSummary.row(eclReturn.contactTelephoneNumber))
        )
    ).flatten
  ).withCssClass("govuk-!-margin-bottom-9")

  def organisationDetails()(implicit request: ReturnDataRequest[_], messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (
        Seq(EclReferenceNumberSummary.row(request.eclRegistrationReference)) ++
          addIf(
            isAmendReturnAndNot(hasRelevantAp12MonthsChanged),
            formatRow(RelevantAp12MonthsSummary.row(eclReturn.relevantAp12Months))
          ) ++
          addIf(
            isAmendReturnAndNot(hasRelevantApLengthChanged),
            formatRow(RelevantApLengthSummary.row(eclReturn.relevantApLength))
          ) ++
          addIf(
            isAmendReturnAndNot(hasUkRevenueChanged),
            formatRow(UkRevenueSummary.row(eclReturn.relevantApRevenue))
          ) ++
          addIf(
            isAmendReturnAndNot(hasCarriedOutAmlRegulatedActivityForFullFyChanged),
            formatRow(AmlRegulatedActivitySummary.row(eclReturn.carriedOutAmlRegulatedActivityForFullFy))
          ) ++
          addIf(
            isAmendReturnAndNot(hasAmlRegulatedActivityLengthChanged),
            formatRow(AmlRegulatedActivityLengthSummary.row(eclReturn.amlRegulatedActivityLength))
          ) ++
          addIf(
            isAmendReturnAndNot(hasCalculatedBandSummaryChanged),
            formatRow(CalculatedBandSummary.row(eclReturn.calculatedLiability))
          ) ++
          addIf(
            isAmendReturnAndNot(hasAmountDueSummaryChanged),
            formatRow(AmountDueSummary.row(eclReturn.calculatedLiability))
          )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  private def addIf[T](condition: Boolean, value: T): Seq[T] = if (condition) Seq(value) else Seq.empty

  private def formatRow(row: Option[SummaryListRow]): Option[SummaryListRow] = row.map(_.copy(actions = None))

  private def isAmendReturnAndNot(condition: Boolean): Boolean =
    isAmendReturn && !condition
}

object AmendReturnPdfViewModel {
  implicit val format: OFormat[AmendReturnPdfViewModel] = Json.format[AmendReturnPdfViewModel]

  implicit val contentType: ContentTypeOf[AmendReturnPdfViewModel] =
    ContentTypeOf[AmendReturnPdfViewModel](Some(ContentTypes.JSON))
  implicit val writes: Writeable[AmendReturnPdfViewModel]          = Writeable(
    Writeable.writeableOf_JsValue.transform.compose(format.writes)
  )
}
