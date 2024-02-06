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
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList

final case class CheckYourAnswersViewModel(
  eclReturn: EclReturn,
  eclReturnSubmission: Option[GetEclReturnSubmissionResponse],
  startAmendUrl: Option[String]
) extends TrackEclReturnChanges {

  def amendedAnswersDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (
        addIf(
          hasAmlRegulatedActivityLengthChanged,
          AmlRegulatedActivityLengthSummary.row(eclReturn.amlRegulatedActivityLength)
        ) ++
          addIf(
            hasRelevantApLengthChanged,
            RelevantApLengthSummary.row(eclReturn.relevantApLength)
          ) ++
          addIf(
            hasUkRevenueChanged,
            UkRevenueSummary.row(eclReturn.relevantApRevenue)
          ) ++
          addIf(
            hasCarriedOutAmlRegulatedActivityForFullFyChanged,
            RelevantAp12MonthsSummary.row(eclReturn.carriedOutAmlRegulatedActivityForFullFy)
          ) ++
          addIf(
            hasAmlRegulatedActivityLengthChanged,
            AmlRegulatedActivityLengthSummary.row(eclReturn.amlRegulatedActivityLength)
          ) ++
          addIf(
            hasCalculatedBandSummaryChanged,
            CalculatedBandSummary.row(eclReturn.calculatedLiability)
          ) ++
          addIf(
            hasAmountDueSummaryChanged,
            AmountDueSummary.row(eclReturn.calculatedLiability)
          ) ++
          addIf(
            hasContactNameChanged,
            ContactNameSummary.row(eclReturn.contactName)
          ) ++
          addIf(
            hasContactRoleChanged,
            ContactRoleSummary.row(eclReturn.contactRole)
          ) ++
          addIf(
            hasContactEmailAddressChanged,
            ContactEmailSummary.row(eclReturn.contactEmailAddress)
          ) ++
          addIf(
            hasContactTelephoneNumberChanged,
            ContactNumberSummary.row(eclReturn.contactTelephoneNumber)
          )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def amendReasonDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        AmendReasonSummary.row(eclReturn.amendReason)
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def contactDetails()(implicit messages: Messages): SummaryList = SummaryListViewModel(
    rows = (
      addIf(
        isFirstTimeReturn || isAmendReturnAndNot(hasContactNameChanged),
        ContactNameSummary.row(eclReturn.contactName)
      ) ++
        addIf(
          isFirstTimeReturn || isAmendReturnAndNot(hasContactRoleChanged),
          ContactRoleSummary.row(eclReturn.contactRole)
        ) ++
        addIf(
          isFirstTimeReturn || isAmendReturnAndNot(hasContactEmailAddressChanged),
          ContactEmailSummary.row(eclReturn.contactEmailAddress)
        ) ++
        addIf(
          isFirstTimeReturn || isAmendReturnAndNot(hasContactTelephoneNumberChanged),
          ContactNumberSummary.row(eclReturn.contactTelephoneNumber)
        )
    ).flatten
  ).withCssClass("govuk-!-margin-bottom-9")

  def eclDetails()(implicit request: ReturnDataRequest[_], messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (
        Seq(EclReferenceNumberSummary.row(request.eclRegistrationReference)) ++
          addIf(
            isFirstTimeReturn || isAmendReturnAndNot(hasRelevantAp12MonthsChanged),
            RelevantAp12MonthsSummary.row(eclReturn.relevantAp12Months)
          ) ++
          addIf(
            isFirstTimeReturn || isAmendReturnAndNot(hasRelevantApLengthChanged),
            RelevantApLengthSummary.row(eclReturn.relevantApLength)
          ) ++
          addIf(
            isFirstTimeReturn || isAmendReturnAndNot(hasUkRevenueChanged),
            UkRevenueSummary.row(eclReturn.relevantApRevenue)
          ) ++
          addIf(
            isFirstTimeReturn || isAmendReturnAndNot(hasCarriedOutAmlRegulatedActivityForFullFyChanged),
            AmlRegulatedActivitySummary.row(eclReturn.carriedOutAmlRegulatedActivityForFullFy)
          ) ++
          addIf(
            isFirstTimeReturn || isAmendReturnAndNot(hasAmlRegulatedActivityLengthChanged),
            AmlRegulatedActivityLengthSummary.row(eclReturn.amlRegulatedActivityLength)
          ) ++
          addIf(
            isFirstTimeReturn || isAmendReturnAndNot(hasCalculatedBandSummaryChanged),
            CalculatedBandSummary.row(eclReturn.calculatedLiability)
          ) ++
          addIf(
            isFirstTimeReturn || isAmendReturnAndNot(hasAmountDueSummaryChanged),
            AmountDueSummary.row(eclReturn.calculatedLiability)
          )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  private def addIf[T](condition: Boolean, value: T): Seq[T] = if (condition) Seq(value) else Seq.empty
}

object CheckYourAnswersViewModel {
  implicit val format: OFormat[CheckYourAnswersViewModel] = Json.format[CheckYourAnswersViewModel]

  implicit val contentType: ContentTypeOf[CheckYourAnswersViewModel] =
    ContentTypeOf[CheckYourAnswersViewModel](Some(ContentTypes.JSON))
  implicit val writes: Writeable[CheckYourAnswersViewModel]          = Writeable(
    Writeable.writeableOf_JsValue.transform.compose(format.writes)
  )
}
