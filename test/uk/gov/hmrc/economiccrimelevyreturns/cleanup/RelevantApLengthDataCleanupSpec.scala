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

class RelevantApLengthDataCleanupSpec extends SpecBase {

  val dataCleanup = new RelevantApLengthDataCleanup

  "cleanup" should {
    "return an ECL return with the calculated liability set to none" in forAll { eclReturn: EclReturn =>
      dataCleanup.cleanup(eclReturn) shouldBe eclReturn.copy(
        calculatedLiability = None
      )
    }
  }

}