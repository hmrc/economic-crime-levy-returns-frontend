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
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, EclReturn, FirstTimeReturn, GetEclReturnSubmissionResponse}
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.govuk.summarylist._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList

final case class CheckYourAnswersViewModel(
  eclReturn: EclReturn,
  eclReturnSubmission: Option[GetEclReturnSubmissionResponse],
  startAmendUrl: Option[String]
) extends TrackEclReturnChanges {

  private def addIf[T](condition: Boolean, value: T): Seq[T] = if (condition) Seq(value) else Seq.empty

  def amendedAnswersDetails()(implicit request: ReturnDataRequest[_], messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (addIf(
        hasAmlRegulatedActivityLengthChanged,
        AmlRegulatedActivityLengthSummary.row(eclReturn.amlRegulatedActivityLength)
      )).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def amendReasonDetails()(implicit request: ReturnDataRequest[_], messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        AmendReasonSummary.row(request.eclReturn.amendReason)
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def contactDetails()(implicit request: ReturnDataRequest[_], messages: Messages): SummaryList = SummaryListViewModel(
    rows = Seq(
      ContactNameSummary.row(request.eclReturn.contactName),
      ContactRoleSummary.row(request.eclReturn.contactRole),
      ContactEmailSummary.row(request.eclReturn.contactEmailAddress),
      ContactNumberSummary.row(request.eclReturn.contactTelephoneNumber)
    ).flatten
  ).withCssClass("govuk-!-margin-bottom-9")

  def eclDetails()(implicit request: ReturnDataRequest[_], messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (
        Seq(EclReferenceNumberSummary.row(request.eclRegistrationReference)) ++
          addIf(
            isFirstTimeReturn || !hasRelevantAp12MonthsChanged,
            RelevantAp12MonthsSummary.row(eclReturn.relevantAp12Months)
          ) ++
          addIf(
            isFirstTimeReturn || !hasRelevantApLengthChanged,
            RelevantApLengthSummary.row(eclReturn.relevantApLength)
          ) ++
          addIf(
            isFirstTimeReturn || !hasUkRevenueChanged,
            UkRevenueSummary.row(eclReturn.relevantApRevenue)
          ) ++
          addIf(
            isFirstTimeReturn || !hasCarriedOutAmlRegulatedActivityForFullFyChanged,
            AmlRegulatedActivitySummary.row(eclReturn.carriedOutAmlRegulatedActivityForFullFy)
          ) ++
          addIf(
            isFirstTimeReturn || !hasAmlRegulatedActivityLengthChanged,
            AmlRegulatedActivityLengthSummary.row(eclReturn.amlRegulatedActivityLength)
          ) ++
          addIf(
            isFirstTimeReturn || !hasCalculatedBandSummaryChanged,
            CalculatedBandSummary.row(eclReturn.calculatedLiability)
          ) ++
          addIf(
            isFirstTimeReturn || !hasAmountDueSummaryChanged,
            AmountDueSummary.row(eclReturn.calculatedLiability)
          )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")
}

object CheckYourAnswersViewModel {
  implicit val format: OFormat[CheckYourAnswersViewModel] = Json.format[CheckYourAnswersViewModel]

  implicit val contentType: ContentTypeOf[CheckYourAnswersViewModel] =
    ContentTypeOf[CheckYourAnswersViewModel](Some(ContentTypes.JSON))
  implicit val writes: Writeable[CheckYourAnswersViewModel]          = Writeable(
    Writeable.writeableOf_JsValue.transform.compose(format.writes)
  )
}
