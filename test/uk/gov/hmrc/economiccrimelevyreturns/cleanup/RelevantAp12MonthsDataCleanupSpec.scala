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

package uk.gov.hmrc.economiccrimelevyreturns.cleanup

import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn

class RelevantAp12MonthsDataCleanupSpec extends SpecBase {

  val dataCleanup = new RelevantAp12MonthsDataCleanup

  "cleanup" should {
    "return an ECL return with the relevant AP length set to none when relevant AP 12 months is true" in forAll {
      eclReturn: EclReturn =>
        val updatedReturn = eclReturn.copy(relevantAp12Months = Some(true))

        dataCleanup.cleanup(updatedReturn) shouldBe updatedReturn.copy(
          relevantApLength = None
        )
    }

    "return an ECL return when relevant AP 12 months is false" in forAll { eclReturn: EclReturn =>
      val updatedReturn = eclReturn.copy(relevantAp12Months = Some(false))

      dataCleanup.cleanup(updatedReturn) shouldBe updatedReturn
    }
  }

}
