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

package uk.gov.hmrc.economiccrimelevyreturns.services

import org.scalacheck.Gen
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase

trait ServiceSpec extends SpecBase {
  val testException = new Exception("error")

  private val base4xxCodeMin = 400
  private val base4xxCodeMax = 499
  private val base5xxCodeMin = 500
  private val base5xxCodeMax = 599

  def getErrorCode(is5xxError: Boolean) =
    is5xxError match {
      case false => Gen.chooseNum[Int](base4xxCodeMin, base4xxCodeMax).suchThat(_ != 404).sample.get
      case true  => Gen.chooseNum[Int](base5xxCodeMin, base5xxCodeMax).sample.get
    }
}
