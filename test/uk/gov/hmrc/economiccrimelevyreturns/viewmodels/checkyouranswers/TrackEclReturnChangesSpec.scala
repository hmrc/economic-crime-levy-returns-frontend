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
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues.{AmlDaysMax, AmlDaysMin, ApDaysMax, ApDaysMin}
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers.TrackEclReturnChanges
import uk.gov.hmrc.economiccrimelevyreturns.{ValidEclReturn, ValidGetEclReturnSubmissionResponse}

class TrackEclReturnChangesSpec extends SpecBase {

  private val testString          = alphaNumericString
  private val alternateTestString = "alternate" + testString

  def defaultEclReturn(validEclReturn: ValidEclReturn): EclReturn =
    validEclReturn.eclReturn.copy(relevantAp12Months = Some(true))

  "hasRelevantAp12MonthsChanged" should {
    "return true when relevantAp12Months is true and numberOfDaysRegulatedActivityTookPlace is None" in forAll {
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

    "return true when relevantAp12Months is false and numberOfDaysRegulatedActivityTookPlace is set to a value" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantAp12Months = Some(false))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              numberOfDaysRegulatedActivityTookPlace = Some(AmlDaysMax)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasRelevantAp12MonthsChanged shouldBe true
    }

    "return true when relevantAp12Months is None and numberOfDaysRegulatedActivityTookPlace is set to a value" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantAp12Months = None)

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              numberOfDaysRegulatedActivityTookPlace = Some(AmlDaysMax)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasRelevantAp12MonthsChanged shouldBe true
    }

    "return false when relevantAp12Months and numberOfDaysRegulatedActivityTookPlace are set to None" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantAp12Months = None)

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

    "return false when relevantAp12Months is true and numberOfDaysRegulatedActivityTookPlace is set to a value" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantAp12Months = Some(true))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              numberOfDaysRegulatedActivityTookPlace = Some(AmlDaysMax)
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
    "return true when relevantApLength and numberOfDaysRegulatedActivityTookPlace are set to different values" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantApLength = Some(AmlDaysMin))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              numberOfDaysRegulatedActivityTookPlace = Some(AmlDaysMax)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasRelevantApLengthChanged shouldBe true
    }

    "return true when relevantApLength is set to a value and numberOfDaysRegulatedActivityTookPlace is set to None" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantApLength = Some(AmlDaysMax))

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

        sut.hasRelevantApLengthChanged shouldBe true
    }

    "return true when relevantApLength is set to None and numberOfDaysRegulatedActivityTookPlace is set to a value" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantApLength = None)

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              numberOfDaysRegulatedActivityTookPlace = Some(AmlDaysMax)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasRelevantApLengthChanged shouldBe true
    }

    "return false when relevantApLength and numberOfDaysRegulatedActivityTookPlace is set to the same value" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantApLength = Some(AmlDaysMax))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              numberOfDaysRegulatedActivityTookPlace = Some(AmlDaysMax)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasRelevantApLengthChanged shouldBe false
    }

    "return false when relevantApLength and numberOfDaysRegulatedActivityTookPlace are both set to None" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantApLength = None)

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

        sut.hasRelevantApLengthChanged shouldBe false
    }
  }

  "hasUkRevenueChanged" should {
    "return true when relevantApRevenue is set and accountingPeriodRevenue is set to a value" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(relevantApRevenue = Some(RevenueMin))

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

        sut.hasUkRevenueChanged shouldBe true
    }

    "return true when relevantApRevenue is set to None and accountingPeriodRevenue is set to a value" in forAll {
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

        sut.hasUkRevenueChanged shouldBe true
    }

    "return false when relevantApRevenue and accountingPeriodRevenue is set to the same value" in forAll {
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

        sut.hasUkRevenueChanged shouldBe false
    }
  }

  "hasCarriedOutAmlRegulatedActivityForFullFyChanged" should {
    "return true when carriedOutAmlRegulatedActivityForFullFy is None and " +
      "numberOfDaysRegulatedActivityTookPlace is set to accountingPeriodLength" in forAll {
        (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
          val eclReturn = defaultEclReturn(validEclReturn)
            .copy(carriedOutAmlRegulatedActivityForFullFy = None)

          val eclReturnSubmission = validEclReturnSubmission.response
            .copy(returnDetails =
              validEclReturnSubmission.response.returnDetails.copy(
                accountingPeriodLength = ApDaysMax,
                numberOfDaysRegulatedActivityTookPlace = Some(ApDaysMax)
              )
            )

          val sut = TestTrackEclReturnChanges(
            eclReturn = eclReturn,
            eclReturnSubmission = Some(eclReturnSubmission)
          )

          sut.hasCarriedOutAmlRegulatedActivityForFullFyChanged shouldBe true
      }

    "return true when carriedOutAmlRegulatedActivityForFullFy is false and " +
      "numberOfDaysRegulatedActivityTookPlace is set to accountingPeriodLength" in forAll {
        (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
          val eclReturn = defaultEclReturn(validEclReturn)
            .copy(carriedOutAmlRegulatedActivityForFullFy = Some(false))

          val eclReturnSubmission = validEclReturnSubmission.response
            .copy(returnDetails =
              validEclReturnSubmission.response.returnDetails.copy(
                accountingPeriodLength = ApDaysMax,
                numberOfDaysRegulatedActivityTookPlace = Some(ApDaysMax)
              )
            )

          val sut = TestTrackEclReturnChanges(
            eclReturn = eclReturn,
            eclReturnSubmission = Some(eclReturnSubmission)
          )

          sut.hasCarriedOutAmlRegulatedActivityForFullFyChanged shouldBe true
      }

    "return true when carriedOutAmlRegulatedActivityForFullFy is true and " +
      "numberOfDaysRegulatedActivityTookPlace is set to not matching accountingPeriodLength" in forAll {
        (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
          val eclReturn = defaultEclReturn(validEclReturn)
            .copy(carriedOutAmlRegulatedActivityForFullFy = Some(true))

          val eclReturnSubmission = validEclReturnSubmission.response
            .copy(returnDetails =
              validEclReturnSubmission.response.returnDetails.copy(
                accountingPeriodLength = ApDaysMax,
                numberOfDaysRegulatedActivityTookPlace = Some(ApDaysMin)
              )
            )

          val sut = TestTrackEclReturnChanges(
            eclReturn = eclReturn,
            eclReturnSubmission = Some(eclReturnSubmission)
          )

          sut.hasCarriedOutAmlRegulatedActivityForFullFyChanged shouldBe true
      }

    "return false when carriedOutAmlRegulatedActivityForFullFy is false and " +
      "accountingPeriodLength is not the same value as numberOfDaysRegulatedActivityTookPlace" in forAll {
        (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
          val eclReturn = defaultEclReturn(validEclReturn)
            .copy(carriedOutAmlRegulatedActivityForFullFy = Some(false))

          val eclReturnSubmission = validEclReturnSubmission.response
            .copy(returnDetails =
              validEclReturnSubmission.response.returnDetails.copy(
                accountingPeriodLength = ApDaysMax,
                numberOfDaysRegulatedActivityTookPlace = Some(ApDaysMin)
              )
            )

          val sut = TestTrackEclReturnChanges(
            eclReturn = eclReturn,
            eclReturnSubmission = Some(eclReturnSubmission)
          )

          sut.hasCarriedOutAmlRegulatedActivityForFullFyChanged shouldBe false
      }

    "return false when carriedOutAmlRegulatedActivityForFullFy is true and " +
      "numberOfDaysRegulatedActivityTookPlace is set to matching accountingPeriodLength" in forAll {
        (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
          val eclReturn = defaultEclReturn(validEclReturn)
            .copy(carriedOutAmlRegulatedActivityForFullFy = Some(true))

          val eclReturnSubmission = validEclReturnSubmission.response
            .copy(returnDetails =
              validEclReturnSubmission.response.returnDetails.copy(
                accountingPeriodLength = ApDaysMax,
                numberOfDaysRegulatedActivityTookPlace = Some(ApDaysMax)
              )
            )

          val sut = TestTrackEclReturnChanges(
            eclReturn = eclReturn,
            eclReturnSubmission = Some(eclReturnSubmission)
          )

          sut.hasCarriedOutAmlRegulatedActivityForFullFyChanged shouldBe false
      }
  }

  "hasAmlRegulatedActivityLengthChanged" should {
    "return true when amlRegulatedActivityLength and " +
      "numberOfDaysRegulatedActivityTookPlace are set to different values" in forAll {
        (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
          val eclReturn = defaultEclReturn(validEclReturn)
            .copy(amlRegulatedActivityLength = Some(AmlDaysMax))

          val eclReturnSubmission = validEclReturnSubmission.response
            .copy(returnDetails =
              validEclReturnSubmission.response.returnDetails.copy(
                numberOfDaysRegulatedActivityTookPlace = Some(AmlDaysMax - 1)
              )
            )

          val sut = TestTrackEclReturnChanges(
            eclReturn = eclReturn,
            eclReturnSubmission = Some(eclReturnSubmission)
          )

          sut.hasAmlRegulatedActivityLengthChanged shouldBe true
      }

    "return true amlRegulatedActivityLength is None and " +
      "numberOfDaysRegulatedActivityTookPlace is set to a value" in forAll {
        (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
          val eclReturn = defaultEclReturn(validEclReturn)
            .copy(amlRegulatedActivityLength = None)

          val eclReturnSubmission = validEclReturnSubmission.response
            .copy(returnDetails =
              validEclReturnSubmission.response.returnDetails.copy(
                numberOfDaysRegulatedActivityTookPlace = Some(AmlDaysMax)
              )
            )

          val sut = TestTrackEclReturnChanges(
            eclReturn = eclReturn,
            eclReturnSubmission = Some(eclReturnSubmission)
          )

          sut.hasAmlRegulatedActivityLengthChanged shouldBe true
      }

    "return true amlRegulatedActivityLength is set to a value and " +
      "numberOfDaysRegulatedActivityTookPlace is set to None" in forAll {
        (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
          val eclReturn = defaultEclReturn(validEclReturn)
            .copy(amlRegulatedActivityLength = Some(AmlDaysMax))

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

          sut.hasAmlRegulatedActivityLengthChanged shouldBe true
      }

    "return false when amlRegulatedActivityLength and " +
      "numberOfDaysRegulatedActivityTookPlace are both set to the same value" in forAll {
        (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
          val eclReturn = defaultEclReturn(validEclReturn)
            .copy(amlRegulatedActivityLength = Some(AmlDaysMax))

          val eclReturnSubmission = validEclReturnSubmission.response
            .copy(returnDetails =
              validEclReturnSubmission.response.returnDetails.copy(
                numberOfDaysRegulatedActivityTookPlace = Some(AmlDaysMax)
              )
            )

          val sut = TestTrackEclReturnChanges(
            eclReturn = eclReturn,
            eclReturnSubmission = Some(eclReturnSubmission)
          )

          sut.hasAmlRegulatedActivityLengthChanged shouldBe false
      }

    "return false when amlRegulatedActivityLength and " +
      "numberOfDaysRegulatedActivityTookPlace are both set to None" in forAll {
        (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
          val eclReturn = defaultEclReturn(validEclReturn)
            .copy(amlRegulatedActivityLength = None)

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

          sut.hasAmlRegulatedActivityLengthChanged shouldBe false
      }
  }

  "hasCalculatedBandSummaryChanged" should {
    "return true when calculatedLiability is set to None and revenueBand is set to a value" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(calculatedLiability = None)

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

    "return true when calculatedBand and revenueBand are set to different values" in forAll {
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

    "return true when calculatedBand and revenueBand are set to the same value" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val calculatedLiability: CalculatedLiability = defaultEclReturn(validEclReturn).calculatedLiability.get

        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(calculatedLiability = Some(calculatedLiability.copy(calculatedBand = Band.Large)))

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              revenueBand = Band.Large
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasCalculatedBandSummaryChanged shouldBe false
    }
  }

  "hasAmountDueSummaryChanged" should {
    "return true when calculatedLiability is None and amountOfEclDutyLiable is set to a value" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(calculatedLiability = None)

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              amountOfEclDutyLiable = BigDecimal(RevenueMax)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasAmountDueSummaryChanged shouldBe true
    }

    "return true when amountDue and amountOfEclDutyLiable have different values set" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val calculatedLiability: CalculatedLiability = defaultEclReturn(validEclReturn).calculatedLiability.get

        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(calculatedLiability =
            Some(calculatedLiability.copy(amountDue = EclAmount(BigDecimal(RevenueMin), apportioned = false)))
          )

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              amountOfEclDutyLiable = BigDecimal(RevenueMax)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasAmountDueSummaryChanged shouldBe true
    }

    "return false when amountDue and amountOfEclDutyLiable have the same value set" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val calculatedLiability: CalculatedLiability = defaultEclReturn(validEclReturn).calculatedLiability.get

        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(calculatedLiability =
            Some(calculatedLiability.copy(amountDue = EclAmount(BigDecimal(RevenueMax), apportioned = false)))
          )

        val eclReturnSubmission = validEclReturnSubmission.response
          .copy(returnDetails =
            validEclReturnSubmission.response.returnDetails.copy(
              amountOfEclDutyLiable = BigDecimal(RevenueMax)
            )
          )

        val sut = TestTrackEclReturnChanges(
          eclReturn = eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )

        sut.hasAmountDueSummaryChanged shouldBe false
    }
  }

  "hasContactNameChanged" should {
    "return true when contactName is None and name is set to a value" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(contactName = None)

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

        sut.hasContactNameChanged shouldBe true
    }

    "return true when contactName and name are set to different values" in forAll {
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

    "return false contactName and name are set to the same values" in forAll {
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
    "return true when contactRole is None and positionInCompany is set to a value" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(contactRole = None)

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

        sut.hasContactRoleChanged shouldBe true
    }

    "return true when contactRole and positionInCompany are set to different values" in forAll {
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

    "return false contactRole and positionInCompany are set to the same values" in forAll {
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
    "return true when contactEmailAddress is None and emailAddress is set to a value" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(contactEmailAddress = None)

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

        sut.hasContactEmailAddressChanged shouldBe true
    }

    "return true when contactEmailAddress and emailAddress are set to different values" in forAll {
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

    "return false contactEmailAddress and emailAddress are set to the same values" in forAll {
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
    "return true when contactTelephoneNumber is None and telephoneNumber is set to a value" in forAll {
      (validEclReturn: ValidEclReturn, validEclReturnSubmission: ValidGetEclReturnSubmissionResponse) =>
        val eclReturn = defaultEclReturn(validEclReturn)
          .copy(contactTelephoneNumber = None)

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

        sut.hasContactTelephoneNumberChanged shouldBe true
    }

    "return true when contactTelephoneNumber and telephoneNumber are set to different values" in forAll {
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

    "return false contactTelephoneNumber and telephoneNumber are set to the same values" in forAll {
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
