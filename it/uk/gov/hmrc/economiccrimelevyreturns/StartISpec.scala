/*
 * Copyright 2022 HM Revenue & Customs
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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models._

import java.time.LocalDate

class StartISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.StartController.start().url}"                  should {
    behave like authorisedActionRoute(routes.StartController.start())

    "response with 200 status and the choose return period view if no obligation details are held in the return data" in {
      stubAuthorised()

      stubGetReturn(EclReturn.empty(testInternalId, Some(FirstTimeReturn)))
      stubGetSessionEmpty()
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.StartController.start()))

      status(result) shouldBe OK
      html(result)     should include("You need to choose a financial year before you submit a return")
    }

    "redirect to the start page if obligation details are held in the return data" in {
      stubAuthorised()

      val obligationDetails = random[ObligationDetails]

      stubGetReturn(
        EclReturn.empty(testInternalId, Some(FirstTimeReturn)).copy(obligationDetails = Some(obligationDetails))
      )
      stubGetSessionEmpty()
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.StartController.start()))

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.StartController.onPageLoad(obligationDetails.periodKey).url)
    }
  }

  s"GET ${routes.StartController.onPageLoad(":periodKey").url}" should {
    behave like authorisedActionRoute(routes.StartController.onPageLoad(testPeriodKey))

    "respond with 200 status and the correct HTML view if the period key is for an open obligation and the due date is 30/09/2023" in {
      stubAuthorised()

      val openObligation = random[ObligationDetails].copy(
        status = Open,
        inboundCorrespondenceFromDate = LocalDate.parse("2022-04-01"),
        inboundCorrespondenceToDate = LocalDate.parse("2023-03-31"),
        inboundCorrespondenceDueDate = LocalDate.parse("2023-09-30"),
        periodKey = testPeriodKey
      )

      val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))
      val emptyReturn    = EclReturn.empty(testInternalId, Some(FirstTimeReturn))

      val eclRegistrationReference = random[String]
      val eclRegistrationDate      = "20230901"

      stubQueryKnownFacts(eclRegistrationReference, eclRegistrationDate)
      stubGetObligations(obligationData)

      stubGetReturn(emptyReturn)
      stubUpsertReturn(emptyReturn.copy(obligationDetails = Some(openObligation)))
      stubGetSessionEmpty()
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.StartController.onPageLoad(openObligation.periodKey)))

      status(result) shouldBe OK
      html(result)     should include("Submit your Economic Crime Levy return for 2022-2023")
    }

    "respond with 200 status and the correct HTML view if the period key is for an open obligation and the due date is 30/09/2024" in {
      stubAuthorised()

      val openObligation = random[ObligationDetails].copy(
        status = Open,
        inboundCorrespondenceFromDate = LocalDate.parse("2023-04-01"),
        inboundCorrespondenceToDate = LocalDate.parse("2024-03-31"),
        inboundCorrespondenceDueDate = LocalDate.parse("2024-09-30"),
        periodKey = testPeriodKey
      )

      val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))
      val emptyReturn    = EclReturn.empty(testInternalId, Some(FirstTimeReturn))

      val eclRegistrationReference = random[String]
      val eclRegistrationDate      = "20230901"

      stubQueryKnownFacts(eclRegistrationReference, eclRegistrationDate)
      stubGetObligations(obligationData)

      stubGetReturn(emptyReturn)
      stubUpsertReturn(emptyReturn.copy(obligationDetails = Some(openObligation)))
      stubGetSessionEmpty()
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.StartController.onPageLoad(openObligation.periodKey)))

      status(result) shouldBe OK
      html(result)     should include("Submit your Economic Crime Levy return for 2023-2024")
    }

    "respond with 200 status and no obligation for period HTML view if there is no obligation for the period key" in {
      stubAuthorised()

      val obligationData = ObligationData(obligations = Seq.empty)

      val eclRegistrationReference = random[String]
      val eclRegistrationDate      = "20230901"

      val eclReturn = random[EclReturn].copy(obligationDetails = None)

      stubQueryKnownFacts(eclRegistrationReference, eclRegistrationDate)
      stubGetObligations(obligationData)
      stubUpsertSession()

      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.periodKey -> testPeriodKey))
      stubGetSession(validSessionData)
      stubGetReturn(eclReturn)

      val result = callRoute(FakeRequest(routes.StartController.onPageLoad(testPeriodKey)))

      status(result) shouldBe OK
      html(result)     should include("You cannot submit a return for this financial year")
    }

    "respond with 200 status and the already submitted return HTML view if the obligation for the period key is fulfilled" in {
      stubAuthorised()

      val openObligation = random[ObligationDetails].copy(
        status = Fulfilled,
        inboundCorrespondenceFromDate = LocalDate.parse("2022-04-01"),
        inboundCorrespondenceToDate = LocalDate.parse("2023-03-31"),
        inboundCorrespondenceDateReceived = Some(LocalDate.parse("2023-04-01")),
        periodKey = testPeriodKey
      )

      val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))
      val eclReturn      = random[EclReturn]
        .copy(internalId = testInternalId, returnType = Some(FirstTimeReturn), obligationDetails = Some(openObligation))

      val eclRegistrationReference = random[String]
      val eclRegistrationDate      = "20230901"

      stubQueryKnownFacts(eclRegistrationReference, eclRegistrationDate)
      stubGetObligations(obligationData)

      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.periodKey -> testPeriodKey))

      stubGetSession(validSessionData)

      stubGetReturn(eclReturn)
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.StartController.onPageLoad(openObligation.periodKey)))

      status(result) shouldBe OK
      html(result)     should include("You have already submitted a return for the 2022-2023 financial year")
    }
  }

}
