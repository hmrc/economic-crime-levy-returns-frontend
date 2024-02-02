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

package uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkyouranswers

import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues.ApDaysMax
import uk.gov.hmrc.economiccrimelevyreturns.models.{Band, CalculatedLiability, EclAmount, EclReturn, GetEclReturnSubmissionResponse}
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers.TrackEclReturnChanges
import uk.gov.hmrc.economiccrimelevyreturns.{ValidEclReturn, ValidGetEclReturnSubmissionResponse}

class TrackEclReturnChangesSpec extends SpecBase {

  private val testString          = alphaNumericString
  private val alternateTestString = "alternate" + alphaNumericString

  def defaultEclReturn(validEclReturn: ValidEclReturn): EclReturn =
    validEclReturn.eclReturn.copy(relevantAp12Months = Some(true))

  "hasRelevantAp12MonthsChanged" should {
    "return true when changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantAp12Months = Some(true))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              numberOfDaysRegulatedActivityTookPlace = None
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasRelevantAp12MonthsChanged shouldBe true
    }

    "return false when not changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantAp12Months = Some(false))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              numberOfDaysRegulatedActivityTookPlace = None
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasRelevantAp12MonthsChanged shouldBe false
    }
  }

  "hasRelevantApLengthChanged" should {
    "return true when changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantApLength = None)

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              numberOfDaysRegulatedActivityTookPlace = Some(ApDaysMax)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasRelevantApLengthChanged shouldBe true
    }

    "return false when not changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantApLength = Some(ApDaysMax))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              numberOfDaysRegulatedActivityTookPlace = Some(ApDaysMax)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasRelevantApLengthChanged shouldBe false
    }
  }

  "hasUkRevenueChanged" should {
    "return true when changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantApRevenue = None)

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              accountingPeriodRevenue = BigDecimal(RevenueMax)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasRelevantApLengthChanged shouldBe true
    }

    "return false when not changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantApRevenue = Some(RevenueMax))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              accountingPeriodRevenue = RevenueMax
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasRelevantApLengthChanged shouldBe false
    }
  }

  "hasCarriedOutAmlRegulatedActivityForFullFyChanged" should {
    "return true when changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(carriedOutAmlRegulatedActivityForFullFy = None)

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              numberOfDaysRegulatedActivityTookPlace = Some(365)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasCarriedOutAmlRegulatedActivityForFullFyChanged shouldBe true
    }
  }

  "hasAmlRegulatedActivityLengthChanged" should {
    "return true when changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(amlRegulatedActivityLength = Some(365))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              numberOfDaysRegulatedActivityTookPlace = Some(364)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasAmlRegulatedActivityLengthChanged shouldBe true
    }
  }

  "hasCalculatedBandSummaryChanged" should {
    "return true when changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val calculatedLiability: CalculatedLiability = defaultEclReturn(validEclReturn).calculatedLiability.get

        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(calculatedLiability = Some(calculatedLiability.copy(calculatedBand = Band.Large)))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              revenueBand = Band.Medium
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasCalculatedBandSummaryChanged shouldBe true
    }
  }

  "hasAmountDueSummaryChanged" should {
    "return true when changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val calculatedLiability: CalculatedLiability = defaultEclReturn(validEclReturn).calculatedLiability.get

        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(calculatedLiability =
            Some(calculatedLiability.copy(amountDue = EclAmount(BigDecimal(1), apportioned = false)))
          )

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              amountOfEclDutyLiable = BigDecimal(2)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasAmountDueSummaryChanged shouldBe true
    }
  }

  "hasContactNameChanged" should {
    "return true when changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(contactName = Some(testString))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(declarationDetails =
            validEclReturnSubmission.response.declarationDetails.copy(
              name = alternateTestString
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasContactNameChanged shouldBe true
    }

    "return false when not changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(contactName = Some(testString))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(declarationDetails =
            validEclReturnSubmission.response.declarationDetails.copy(
              name = testString
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasContactNameChanged shouldBe false
    }
  }

  "hasContactRoleChanged" should {
    "return true when changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(contactRole = Some(testString))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(declarationDetails =
            validEclReturnSubmission.response.declarationDetails.copy(
              positionInCompany = alternateTestString
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasContactRoleChanged shouldBe true
    }

    "return false when not changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(contactRole = Some(testString))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(declarationDetails =
            validEclReturnSubmission.response.declarationDetails.copy(
              positionInCompany = testString
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasContactRoleChanged shouldBe false
    }
  }

  "hasContactEmailAddressChanged" should {
    "return true when changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(contactEmailAddress = Some(testString))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(declarationDetails =
            validEclReturnSubmission.response.declarationDetails.copy(
              emailAddress = alternateTestString
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasContactEmailAddressChanged shouldBe true
    }

    "return false when not changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(contactEmailAddress = Some(testString))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(declarationDetails =
            validEclReturnSubmission.response.declarationDetails.copy(
              emailAddress = testString
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasContactEmailAddressChanged shouldBe false
    }
  }

  "hasContactTelephoneNumberChanged" should {
    "return true when changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(contactTelephoneNumber = Some(testString))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(declarationDetails =
            validEclReturnSubmission.response.declarationDetails.copy(
              telephoneNumber = alternateTestString
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasContactTelephoneNumberChanged shouldBe true
    }

    "return false when not changed" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(contactTelephoneNumber = Some(testString))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(declarationDetails =
            validEclReturnSubmission.response.declarationDetails.copy(
              telephoneNumber = testString
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasContactTelephoneNumberChanged shouldBe false
    }
  }
}

final case class TestTrackEclReturnChanges(
  eclReturn: EclReturn,
  eclReturnSubmission: Option[GetEclReturnSubmissionResponse]
) extends TrackEclReturnChanges
