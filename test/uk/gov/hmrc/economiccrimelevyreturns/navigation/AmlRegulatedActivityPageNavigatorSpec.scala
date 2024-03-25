/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, Mode}

class AmlRegulatedActivityPageNavigatorSpec extends SpecBase {

  val pageNavigator = new AmlRegulatedActivityPageNavigator()

  "nextPage" should {
    "return a Call to the next page in all modes" in forAll {
      (eclReturn: EclReturn, amlRegulatedActivity: Boolean, mode: Mode) =>
        val updatedReturn: EclReturn =
          eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = Some(amlRegulatedActivity))

        val nextPage = amlRegulatedActivity match {
          case true  => routes.AmountDueController.onPageLoad(mode)
          case false => routes.AmlRegulatedActivityLengthController.onPageLoad(mode)
        }

        pageNavigator.nextPage(mode, updatedReturn) shouldBe nextPage
    }

    "return a Call to the error page if no data" in forAll { (eclReturn: EclReturn, mode: Mode) =>
      val updatedReturn: EclReturn = eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = None)

      pageNavigator.nextPage(mode, updatedReturn) shouldBe
        routes.NotableErrorController.answersAreInvalid()
    }
  }

}
