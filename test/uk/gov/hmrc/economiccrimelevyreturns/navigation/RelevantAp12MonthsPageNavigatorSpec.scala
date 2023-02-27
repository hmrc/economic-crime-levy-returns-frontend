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

import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._

class RelevantAp12MonthsPageNavigatorSpec extends SpecBase {

  val pageNavigator = new RelevantAp12MonthsPageNavigator

  "nextPage" should {
    "return a Call to the UK revenue page from the relevant AP 12 months page in NormalMode when the 'Yes' option is selected" in forAll {
      eclReturn: EclReturn =>
        val updatedReturn = eclReturn.copy(relevantAp12Months = Some(true))

        pageNavigator.nextPage(NormalMode, updatedReturn) shouldBe routes.UkRevenueController.onPageLoad(NormalMode)
    }

    "return a Call to the relevant AP length page from the relevant AP 12 months page in NormalMode when the 'No' option is selected" in forAll {
      eclReturn: EclReturn =>
        //TODO Implement call and assertion when building the next page
    }
  }

}
