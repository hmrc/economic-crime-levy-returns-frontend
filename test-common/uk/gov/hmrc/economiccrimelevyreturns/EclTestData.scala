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

package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.{MinMaxValues, Regex}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.generators.Generators
import uk.gov.hmrc.economiccrimelevyreturns.models.Band.Medium
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn, FirstTimeReturn, GetEclReturnChargeDetails, GetEclReturnDeclarationDetails, GetEclReturnDetails, GetEclReturnSubmissionResponse, ObligationDetails, SessionData}

import java.time.{Instant, LocalDate}
import scala.math.BigDecimal.RoundingMode

case class ValidPeriodKey(periodKey: String)

case class EnrolmentsWithEcl(enrolments: Enrolments)

case class EnrolmentsWithoutEcl(enrolments: Enrolments)

final case class EclLiabilityCalculationData(
  relevantApLength: Int,
  relevantApRevenue: BigDecimal,
  amlRegulatedActivityLength: Int
)

final case class ValidEclReturn(eclReturn: EclReturn, eclLiabilityCalculationData: EclLiabilityCalculationData)

final case class ValidGetEclReturnSubmissionResponse(response: GetEclReturnSubmissionResponse)

trait EclTestData { self: Generators =>

  val FullYear: Int      = 365
  val MinRevenue: Double = 0.00
  val MaxRevenue: Double = 99999999999.99
  val MinAmountDue       = 0
  val MaxAmountDue       = 250000
  val PeriodKeyLength    = 4
  val RevenueMin: Double = 0.0
  val RevenueMax: Double = 99999999999.99

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary {
    Instant.now()
  }

  implicit val arbLocalDate: Arbitrary[LocalDate] = Arbitrary {
    LocalDate.now()
  }

  implicit val arbEnrolmentsWithEcl: Arbitrary[EnrolmentsWithEcl] = Arbitrary {
    for {
      enrolments               <- Arbitrary.arbitrary[Enrolments]
      enrolment                <- Arbitrary.arbitrary[Enrolment]
      eclRegistrationReference <- Arbitrary.arbitrary[String]
      eclEnrolmentIdentifier    = EnrolmentIdentifier(EclEnrolment.IdentifierKey, eclRegistrationReference)
      eclEnrolment              =
        enrolment.copy(key = EclEnrolment.ServiceName, identifiers = enrolment.identifiers :+ eclEnrolmentIdentifier)
    } yield EnrolmentsWithEcl(enrolments.copy(enrolments.enrolments + eclEnrolment))
  }

  implicit val arbEnrolmentsWithoutEcl: Arbitrary[EnrolmentsWithoutEcl] = Arbitrary {
    Arbitrary
      .arbitrary[Enrolments]
      .retryUntil(
        !_.enrolments.exists(e =>
          e.key == EclEnrolment.ServiceName && e.identifiers.exists(_.key == EclEnrolment.IdentifierKey)
        )
      )
      .map(EnrolmentsWithoutEcl)
  }

  implicit val arbPeriodKey: Arbitrary[ValidPeriodKey] = Arbitrary {
    Gen.listOfN(PeriodKeyLength, Gen.alphaNumChar).map(_.mkString).map(ValidPeriodKey)
  }

  implicit def arbRevenue: Arbitrary[BigDecimal] =
    Arbitrary {
      Gen.chooseNum[Double](RevenueMin, RevenueMax).map(BigDecimal.apply(_).setScale(2, RoundingMode.DOWN))
    }

  implicit val arbValidEclReturn: Arbitrary[ValidEclReturn] = Arbitrary {
    for {
      relevantAp12Months                      <- Arbitrary.arbitrary[Boolean]
      relevantApLength                        <- Gen.chooseNum[Int](MinMaxValues.ApDaysMin, MinMaxValues.ApDaysMax)
      relevantApRevenue                       <- arbRevenue.arbitrary
      carriedOutAmlRegulatedActivityForFullFy <- Arbitrary.arbitrary[Boolean]
      amlRegulatedActivityLength              <- Gen.chooseNum[Int](MinMaxValues.AmlDaysMin, MinMaxValues.AmlDaysMax)
      liabilityAmountDue                      <-
        Gen.chooseNum[Double](MinAmountDue, MaxAmountDue).map(BigDecimal.apply(_).setScale(2, RoundingMode.DOWN))
      calculatedLiability                     <-
        Arbitrary
          .arbitrary[CalculatedLiability]
          .map(calcLiability =>
            calcLiability
              .copy(calculatedBand = Medium, amountDue = calcLiability.amountDue.copy(amount = liabilityAmountDue))
          )
      contactName                             <- stringsWithMaxLength(MinMaxValues.NameMaxLength)
      contactRole                             <- stringsWithMaxLength(MinMaxValues.RoleMaxLength)
      contactEmailAddress                     <- emailAddress(MinMaxValues.EmailMaxLength)
      contactTelephoneNumber                  <- stringFromRegex(MinMaxValues.TelephoneNumberMaxLength, Regex.TelephoneNumberRegex)
      obligationDetails                       <- Arbitrary.arbitrary[ObligationDetails]
      internalId                               = alphaNumericString
    } yield ValidEclReturn(
      EclReturn
        .empty(internalId = internalId, Some(FirstTimeReturn))
        .copy(
          relevantAp12Months = Some(relevantAp12Months),
          relevantApLength = if (relevantAp12Months) None else Some(relevantApLength),
          relevantApRevenue = Some(relevantApRevenue),
          carriedOutAmlRegulatedActivityForFullFy = Some(carriedOutAmlRegulatedActivityForFullFy),
          amlRegulatedActivityLength =
            if (carriedOutAmlRegulatedActivityForFullFy) None else Some(amlRegulatedActivityLength),
          calculatedLiability = Some(calculatedLiability),
          contactName = Some(contactName),
          contactRole = Some(contactRole),
          contactEmailAddress = Some(contactEmailAddress),
          contactTelephoneNumber = Some(contactTelephoneNumber),
          obligationDetails = Some(obligationDetails)
        ),
      EclLiabilityCalculationData(
        relevantApLength = if (relevantAp12Months) FullYear else relevantApLength,
        relevantApRevenue = relevantApRevenue,
        amlRegulatedActivityLength =
          if (carriedOutAmlRegulatedActivityForFullFy) FullYear else amlRegulatedActivityLength
      )
    )
  }

  def alphaNumericString: String = Gen.alphaNumStr.sample.get

  val testInternalId: String               = alphaNumericString
  val testEclRegistrationReference: String = alphaNumericString
  val testChargeReference: String          = alphaNumericString
  val testPeriodKey: String                = arbPeriodKey.arbitrary.sample.get.periodKey
  val UkRevenueThreshold: Long             = 10200000L

  implicit val arbSessionData: Arbitrary[SessionData] =
    Arbitrary {
      for {
        id     <- Gen.uuid
        values <- Arbitrary.arbitrary[Map[String, String]]
      } yield SessionData(id.toString, values, None)
    }

  implicit val arbValidGetEclReturnSubmissionResponse: Arbitrary[ValidGetEclReturnSubmissionResponse] = Arbitrary {
    for {
      accountingPeriodRevenue <- bigDecimalInRange(MinRevenue, MaxRevenue)
      amountOfEclDutyLiable   <- bigDecimalInRange(MinAmountDue, MaxAmountDue)
      chargeDetails           <- Arbitrary.arbitrary[GetEclReturnChargeDetails]
      declarationDetails      <- Arbitrary.arbitrary[GetEclReturnDeclarationDetails]
      eclReference            <- Arbitrary.arbitrary[String]
      processingDateTime      <- Arbitrary.arbitrary[Instant]
      returnDetails           <- Arbitrary.arbitrary[GetEclReturnDetails]
      submissionId            <- Arbitrary.arbitrary[String]
    } yield ValidGetEclReturnSubmissionResponse(
      GetEclReturnSubmissionResponse(
        chargeDetails = chargeDetails,
        declarationDetails = declarationDetails,
        eclReference = eclReference,
        processingDateTime = processingDateTime,
        returnDetails = returnDetails
          .copy(accountingPeriodRevenue = accountingPeriodRevenue, amountOfEclDutyLiable = amountOfEclDutyLiable),
        submissionId = Some(submissionId)
      )
    )
  }
}
