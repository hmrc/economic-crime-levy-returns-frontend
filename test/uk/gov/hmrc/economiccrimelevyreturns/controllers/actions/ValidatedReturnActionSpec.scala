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

package uk.gov.hmrc.economiccrimelevyreturns.controllers.actions

import org.mockito.ArgumentMatchers.any
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest

import scala.concurrent.Future

class ValidatedReturnActionSpec extends SpecBase {

  val mockEclReturnsConnector: EclReturnsConnector = mock[EclReturnsConnector]

  val validatedReturnAction = new ValidatedReturnActionImpl(mockEclReturnsConnector)

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  "filter" should {
    "return None if the ECL return data is valid" in forAll { (internalId: String, eclReturn: EclReturn) =>
      when(mockEclReturnsConnector.getReturnValidationErrors(any())(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Option[Result]] =
        validatedReturnAction.filter(ReturnDataRequest(fakeRequest, internalId, eclReturn))

      await(result) shouldBe None
    }

    "redirect to the journey recovery page if the ECL return data is invalid" in forAll {
      (internalId: String, eclReturn: EclReturn, dataValidationErrors: DataValidationErrors) =>
        when(mockEclReturnsConnector.getReturnValidationErrors(any())(any()))
          .thenReturn(Future.successful(Some(dataValidationErrors)))

        val result: Future[Option[Result]] =
          validatedReturnAction.filter(ReturnDataRequest(fakeRequest, internalId, eclReturn))

        await(result) shouldBe Some(Redirect(routes.NotableErrorController.answersAreInvalid()))
    }
  }

}
