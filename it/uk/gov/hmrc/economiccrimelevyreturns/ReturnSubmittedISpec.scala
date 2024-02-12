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
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues.EmailMaxLength
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{ObligationDetails, SessionKeys}

class ReturnSubmittedISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.ReturnSubmittedController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.ReturnSubmittedController.onPageLoad())

    "respond with 200 status and the return submitted HTML view when it is not a nil return" in {
      stubAuthorised()

      val chargeReference   = random[String]
      val email             = emailAddress(EmailMaxLength).sample.get
      val obligationDetails = random[ObligationDetails]
      val amountDue         = "10000"

      stubDeleteReturn()
      stubDeleteSession()

      val result = callRoute(
        FakeRequest(routes.ReturnSubmittedController.onPageLoad())
          .withSession(
            (SessionKeys.ChargeReference, chargeReference),
            (SessionKeys.Email, email),
            (SessionKeys.ObligationDetails, Json.toJson(obligationDetails).toString()),
            (SessionKeys.AmountDue, amountDue)
          )
      )

      status(result) shouldBe OK
      html(result)     should include("Return submitted")
      html(result)     should include("ECL return number")
    }

    "respond with 200 status and the nil return submitted HTML view when it is a nil return" in {
      stubAuthorised()

      val email             = random[String]
      val obligationDetails = random[ObligationDetails]
      val amountDue         = "10000"

      stubDeleteReturn()
      stubDeleteSession()

      val result = callRoute(
        FakeRequest(routes.ReturnSubmittedController.onPageLoad())
          .withSession(
            (SessionKeys.Email, email),
            (SessionKeys.ObligationDetails, Json.toJson(obligationDetails).toString()),
            (SessionKeys.AmountDue, amountDue)
          )
      )

      status(result) shouldBe OK
      html(result)     should include("Return submitted")
      html(result)     should not include "ECL return number"
    }
  }

}
