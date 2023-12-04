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
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn

class NotableErrorISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.NotableErrorController.answersAreInvalid().url}"       should {
    behave like authorisedActionRoute(routes.NotableErrorController.answersAreInvalid())

    "respond with 200 status and the answers are invalid HTML view" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]

      stubGetReturn(eclReturn)
      stubGetSessionEmpty()

      val result = callRoute(FakeRequest(routes.NotableErrorController.answersAreInvalid()))

      status(result) shouldBe OK
      html(result)     should include("The answers you provided are not valid")
    }
  }

  s"GET ${routes.NotableErrorController.notRegistered().url}"           should {
    "respond with 303 status" in {
      stubAuthorised()

      val result = callRoute(FakeRequest(routes.NotableErrorController.notRegistered()))

      status(result) shouldBe SEE_OTHER
    }
  }

  s"GET ${routes.NotableErrorController.agentCannotSubmitReturn().url}" should {
    "respond with 200 status and the agent cannot submit return HTML view" in {
      stubAuthorised()

      val result = callRoute(FakeRequest(routes.NotableErrorController.agentCannotSubmitReturn()))

      status(result) shouldBe OK
      html(result)     should include("You cannot use this service to submit an Economic Crime Levy return")
    }
  }
}
