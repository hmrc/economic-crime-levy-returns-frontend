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

      stubGetReturn(EclReturn.empty(testInternalId))

      val result = callRoute(FakeRequest(routes.StartController.start()))

      status(result) shouldBe OK
      html(result)     should include("You need to choose a financial year before you submit a return")
    }

    "redirect to the start page if obligation details are held in the return data" in {
      stubAuthorised()

      val obligationDetails = random[ObligationDetails]

      stubGetReturn(EclReturn.empty(testInternalId).copy(obligationDetails = Some(obligationDetails)))

      val result = callRoute(FakeRequest(routes.StartController.start()))

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.StartController.onPageLoad(obligationDetails.periodKey).url)
    }
  }

  s"GET ${routes.StartController.onPageLoad(":periodKey").url}" should {
    behave like authorisedActionRoute(routes.StartController.onPageLoad(random[String]))

    "respond with 200 status and the start HTML view if the period key is for an open obligation" in {
      stubAuthorised()

      val openObligation = random[ObligationDetails].copy(
        status = Open,
        inboundCorrespondenceFromDate = LocalDate.parse("2022-04-01"),
        inboundCorrespondenceToDate = LocalDate.parse("2023-03-31")
      )

      val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))
      val emptyReturn    = EclReturn.empty(testInternalId)

      val eclRegistrationReference = random[String]
      val eclRegistrationDate      = "20230901"

      stubQueryKnownFacts(eclRegistrationReference, eclRegistrationDate)
      stubGetObligations(obligationData)

      stubGetReturn(emptyReturn)
      stubUpsertReturn(emptyReturn.copy(obligationDetails = Some(openObligation)))

      val result = callRoute(FakeRequest(routes.StartController.onPageLoad(openObligation.periodKey)))

      status(result) shouldBe OK
      html(result)     should include("Submit your Economic Crime Levy return for 2022-2023")
    }

    "respond with 200 status and the no obligation for period HTML view if there is no obligation for the period key" in {
      stubAuthorised()

      val periodKey      = "22XY"
      val obligationData = ObligationData(obligations = Seq.empty)

      val eclRegistrationReference = random[String]
      val eclRegistrationDate      = "20230901"

      stubQueryKnownFacts(eclRegistrationReference, eclRegistrationDate)
      stubGetObligations(obligationData)

      val result = callRoute(FakeRequest(routes.StartController.onPageLoad(periodKey)))

      status(result) shouldBe OK
      html(result)     should include("You cannot submit a return for this financial year")
    }

    "respond with 200 status and the already submitted return HTML view if the obligation for the period key is fulfilled" in {
      stubAuthorised()

      val openObligation = random[ObligationDetails].copy(
        status = Fulfilled,
        inboundCorrespondenceFromDate = LocalDate.parse("2022-04-01"),
        inboundCorrespondenceToDate = LocalDate.parse("2023-03-31"),
        inboundCorrespondenceDateReceived = Some(LocalDate.parse("2023-04-01"))
      )

      val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))
      val emptyReturn    = EclReturn.empty(testInternalId)

      val eclRegistrationReference = random[String]
      val eclRegistrationDate      = "20230901"

      stubQueryKnownFacts(eclRegistrationReference, eclRegistrationDate)
      stubGetObligations(obligationData)

      stubGetReturn(emptyReturn)
      stubUpsertReturn(emptyReturn.copy(obligationDetails = Some(openObligation)))

      val result = callRoute(FakeRequest(routes.StartController.onPageLoad(openObligation.periodKey)))

      status(result) shouldBe OK
      html(result)     should include("You have already submitted a return for the 2022-2023 financial year")
    }
  }

}
