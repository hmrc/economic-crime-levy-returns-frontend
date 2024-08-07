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

package uk.gov.hmrc.economiccrimelevyreturns.generators

import org.scalacheck.Arbitrary
import org.scalacheck.derive.MkArbitrary
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.economiccrimelevyreturns.EclTestData
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.QueryKnownFactsResponse
import uk.gov.hmrc.economiccrimelevyreturns.models.email.{AmendReturnSubmittedParameters, ReturnSubmittedEmailParameters}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataValidationError, ErrorCode}
import uk.gov.hmrc.economiccrimelevyreturns.models._
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary

object CachedArbitraries extends EclTestData with Generators {

  private def mkArb[T](implicit mkArb: MkArbitrary[T]): Arbitrary[T] = MkArbitrary[T].arbitrary

  implicit lazy val arbAmendReturnSubmittedEmailParameters: Arbitrary[AmendReturnSubmittedParameters] = mkArb
  implicit lazy val arbBand: Arbitrary[Band]                                                          = mkArb
  implicit lazy val arbCalculatedLiability: Arbitrary[CalculatedLiability]                            = mkArb
  implicit lazy val arbCalculateLiabilityRequest: Arbitrary[CalculateLiabilityRequest]                = mkArb
  implicit lazy val arbDataValidationError: Arbitrary[DataValidationError]                            = mkArb
  implicit lazy val arbEclReturn: Arbitrary[EclReturn]                                                = mkArb
  implicit lazy val arbEnrolment: Arbitrary[Enrolment]                                                = mkArb
  implicit lazy val arbEnrolments: Arbitrary[Enrolments]                                              = mkArb
  implicit lazy val arbErrorCode: Arbitrary[ErrorCode]                                                = mkArb
  implicit lazy val arbGetSubscription: Arbitrary[GetSubscriptionResponse]                            = mkArb
  implicit lazy val arbMode: Arbitrary[Mode]                                                          = mkArb
  implicit lazy val arbObligationData: Arbitrary[ObligationData]                                      = mkArb
  implicit lazy val arbObligationDetails: Arbitrary[ObligationDetails]                                = mkArb
  implicit lazy val arbQueryKnownFactsResponse: Arbitrary[QueryKnownFactsResponse]                    = mkArb
  implicit lazy val arbReturnSubmittedEmailParameters: Arbitrary[ReturnSubmittedEmailParameters]      = mkArb
  implicit lazy val arbReturnType: Arbitrary[ReturnType]                                              = mkArb
  implicit lazy val arbSubmitEclReturnResponse: Arbitrary[SubmitEclReturnResponse]                    = mkArb
}
