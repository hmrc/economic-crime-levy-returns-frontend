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

package uk.gov.hmrc.economiccrimelevyreturns.navigation

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn, Mode}
import uk.gov.hmrc.economiccrimelevyreturns.services.EclLiabilityService

import scala.concurrent.Future

class AmlRegulatedActivityLengthPageNavigatorSpec extends SpecBase {

  val pageNavigator = new AmlRegulatedActivityLengthPageNavigator

  "nextPage" should {
    "return a Call to the ECL amount due page in either mode when the ECL return data is valid" in forAll {
      (eclReturn: EclReturn, length: Int, mode: Mode) =>
        val updatedReturn = eclReturn.copy(amlRegulatedActivityLength = Some(length))

        pageNavigator.nextPage(mode, updatedReturn) shouldBe routes.AmountDueController.onPageLoad(mode)
    }
  }

}
