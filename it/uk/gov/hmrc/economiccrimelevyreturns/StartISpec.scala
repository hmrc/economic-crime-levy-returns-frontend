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

class StartISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.StartController.onPageLoad(":periodKey").url}" should {
    behave like authorisedActionRoute(routes.StartController.onPageLoad(random[String]))

    "respond with 200 status and the start HTML view if the period key is for an open obligation" in {
      stubAuthorised()

      val periodKey                = random[String]
      val eclRegistrationReference = random[String]
      val eclRegistrationDate      = "20230901"

      stubQueryKnownFacts(eclRegistrationReference, eclRegistrationDate)

      val result = callRoute(FakeRequest(routes.StartController.onPageLoad(periodKey)))

      status(result) shouldBe OK
      html(result)     should include("Submit your Economic Crime Levy return")
    }
  }

}
