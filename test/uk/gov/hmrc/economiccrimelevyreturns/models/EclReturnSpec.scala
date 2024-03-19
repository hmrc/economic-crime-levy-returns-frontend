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

package uk.gov.hmrc.economiccrimelevyreturns.models

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._

class EclReturnSpec extends SpecBase {

  def returnWithNoContactInfo =
    returnWithContactInfo(false, false, false, false)

  def returnWithContactInfo(hasName: Boolean, hasRole: Boolean, hasEmail: Boolean, hasNumber: Boolean) = {
    def value(isPresent: Boolean) =
      if (isPresent) Some(random[String]) else None

    random[EclReturn].copy(
      contactName = value(hasName),
      contactRole = value(hasRole),
      contactEmailAddress = value(hasEmail),
      contactTelephoneNumber = value(hasNumber)
    )
  }

  "hasContactInfo" should {
    "return false if no contact info" in {
      returnWithNoContactInfo.hasContactInfo shouldBe false
    }

    "return true if at least ome of the contact fields is set" in forAll {
      (hasName: Boolean, hasRole: Boolean, hasEmail: Boolean, hasNumber: Boolean) =>
        if (hasName || hasRole || hasEmail || hasNumber) {
          returnWithContactInfo(hasName, hasRole, hasEmail, hasNumber).hasContactInfo shouldBe true
        }
    }
  }
}
