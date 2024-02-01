package uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkyouranswers

import uk.gov.hmrc.economiccrimelevyreturns.{ValidEclReturn, ValidGetEclReturnSubmissionResponse}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn, GetEclReturnSubmissionResponse}
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers.TrackEclReturnChanges

class TrackEclReturnChangesSpec extends SpecBase {

  def createSut(eclReturn: EclReturn, getEclReturnSubmissionResponse: GetEclReturnSubmissionResponse): TrackEclReturnChanges = {
    new {
    } with TrackEclReturnChanges {
      override val eclReturn: EclReturn = eclReturn
      override val eclReturnSubmission: Option[GetEclReturnSubmissionResponse] = Some(getEclReturnSubmissionResponse)
    }
  }

  "hasRelevantAp12MonthsChanged" should {
    "return true when changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>

        val sut = createSut(validEclReturn.eclReturn, validEclReturnSubmission.response)

        sut.isAmendReturn shouldBe true
    }

    "return false when not changed" in forAll {
    }
  }

//  "hasRelevantApLengthChanged" should {
//    "return true when changed" in forAll {
//    }
//
//    "return false when not changed" in forAll {
//    }
//  }
//
//  "hasUkRevenueChanged" should {
//    "return true when changed" in forAll {
//    }
//
//    "return false when not changed" in forAll {
//    }
//  }
//
//  "hasCarriedOutAmlRegulatedActivityForFullFyChanged" should {
//    "return true when changed" in forAll {
//    }
//
//    "return false when not changed" in forAll {
//    }
//  }
//
//  "hasAmlRegulatedActivityLengthChanged" should {
//    "return true when changed" in forAll {
//    }
//
//    "return false when not changed" in forAll {
//    }
//  }
//
//  "hasCalculatedBandSummaryChanged" should {
//    "return true when changed" in forAll {
//    }
//
//    "return false when not changed" in forAll {
//    }
//  }
//
//  "hasAmountDueSummaryChanged" should {
//    "return true when changed" in forAll {
//    }
//
//    "return false when not changed" in forAll {
//    }
//  }
}
