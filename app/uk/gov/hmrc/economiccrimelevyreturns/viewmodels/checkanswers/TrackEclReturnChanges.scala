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

import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, EclReturn, FirstTimeReturn, GetEclReturnSubmissionResponse}

trait TrackEclReturnChanges {

  val eclReturn: EclReturn

  val eclReturnSubmission: Option[GetEclReturnSubmissionResponse]

  val isAmendReturn: Boolean     = eclReturn.returnType.contains(AmendReturn)
  val isFirstTimeReturn: Boolean = eclReturn.returnType.contains(FirstTimeReturn)

  val hasRelevantAp12MonthsChanged: Boolean = eclReturnSubmission match {
    case Some(submission) =>
      submission.returnDetails.numberOfDaysRegulatedActivityTookPlace match {
        case Some(_) => eclReturn.relevantAp12Months.isEmpty || eclReturn.relevantAp12Months.contains(false)
        case None    => eclReturn.relevantAp12Months.contains(true)
      }
    case None             => false
  }

  val hasRelevantApLengthChanged: Boolean = eclReturnSubmission match {
    case Some(submission) =>
      submission.returnDetails.numberOfDaysRegulatedActivityTookPlace match {
        case Some(numOfDays) => !eclReturn.relevantApLength.contains(numOfDays)
        case None            => eclReturn.relevantApLength.isDefined
      }
    case None             => false
  }

  val hasUkRevenueChanged: Boolean = eclReturnSubmission match {
    case Some(submission) =>
      eclReturn.relevantApRevenue match {
        case Some(relevantApRevenue) => relevantApRevenue != submission.returnDetails.accountingPeriodRevenue
        case None                    => true
      }
    case None             => false
  }

  val hasCarriedOutAmlRegulatedActivityForFullFyChanged: Boolean = eclReturnSubmission match {
    case Some(submission) =>
      eclReturn.carriedOutAmlRegulatedActivityForFullFy match {
        case Some(_) =>
          submission.returnDetails.numberOfDaysRegulatedActivityTookPlace match {
            case Some(numberOfDays) =>
              eclReturn.amlRegulatedActivityLength match {
                case Some(length) => numberOfDays != length
                case None         => true
              }
            case None               => true
          }
        case None    =>
          eclReturn.amlRegulatedActivityLength != submission.returnDetails.numberOfDaysRegulatedActivityTookPlace
      }
    case None             => false
  }

  val hasAmlRegulatedActivityLengthChanged: Boolean = eclReturnSubmission match {
    case Some(submission) =>
      eclReturn.amlRegulatedActivityLength match {
        case Some(amlRegulatedActivityLength) =>
          submission.returnDetails.numberOfDaysRegulatedActivityTookPlace match {
            case Some(numberOfDaysRegulatedActivityTookPlace) =>
              amlRegulatedActivityLength != numberOfDaysRegulatedActivityTookPlace
            case None                                         => true
          }
        case None                             => submission.returnDetails.numberOfDaysRegulatedActivityTookPlace.isDefined
      }
    case None             => false
  }

  val hasCalculatedBandSummaryChanged: Boolean = eclReturnSubmission match {
    case Some(submission) =>
      eclReturn.calculatedLiability match {
        case Some(calculatedLiability) => submission.returnDetails.revenueBand != calculatedLiability.calculatedBand
        case None                      => false
      }
    case None             => false
  }

  val hasAmountDueSummaryChanged: Boolean = eclReturnSubmission match {
    case Some(submission) =>
      eclReturn.calculatedLiability match {
        case Some(calculatedLiability) =>
          submission.returnDetails.amountOfEclDutyLiable != calculatedLiability.amountDue.amount
        case None                      => false
      }
    case None             => false
  }

  val hasContactNameChanged: Boolean = eclReturnSubmission match {
    case Some(submission) =>
      eclReturn.contactName match {
        case Some(contactName) => !submission.declarationDetails.name.contains(contactName)
        case None              => true
      }
    case None             => false
  }

  val hasContactRoleChanged: Boolean = eclReturnSubmission match {
    case Some(submission) =>
      eclReturn.contactRole match {
        case Some(contactRole) => !submission.declarationDetails.positionInCompany.contains(contactRole)
        case None              => true
      }
    case None             => false
  }

  val hasContactEmailAddressChanged: Boolean = eclReturnSubmission match {
    case Some(submission) =>
      eclReturn.contactEmailAddress match {
        case Some(contactEmailAddress) => !submission.declarationDetails.emailAddress.contains(contactEmailAddress)
        case None                      => true
      }
    case None             => false
  }

  val hasContactTelephoneNumberChanged: Boolean = eclReturnSubmission match {
    case Some(submission) =>
      eclReturn.contactTelephoneNumber match {
        case Some(contactTelephoneNumber) =>
          !submission.declarationDetails.telephoneNumber.contains(contactTelephoneNumber)
        case None                         => true
      }
    case None             => false
  }

  val hasAnyAmendments: Boolean = Seq(
    hasRelevantAp12MonthsChanged,
    hasRelevantApLengthChanged,
    hasUkRevenueChanged,
    hasCarriedOutAmlRegulatedActivityForFullFyChanged,
    hasAmlRegulatedActivityLengthChanged,
    hasCalculatedBandSummaryChanged,
    hasAmountDueSummaryChanged,
    hasContactNameChanged,
    hasContactRoleChanged,
    hasContactEmailAddressChanged,
    hasContactTelephoneNumberChanged
  ).contains(true)

  val hasAllContactDetailsChanged: Boolean = Seq(
    hasContactNameChanged,
    hasContactRoleChanged,
    hasContactEmailAddressChanged,
    hasContactTelephoneNumberChanged
  ).forall(_ == true)

  val hasAmendReason: Boolean = eclReturn.amendReason.isDefined

  def isAmendReturnAndNot(condition: Boolean): Boolean =
    isAmendReturn && !condition
}
