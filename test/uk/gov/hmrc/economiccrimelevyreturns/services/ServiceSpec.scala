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

  private val base4xxCode   = 400
  private val base5xxCode   = 500
  private val minErrorRange = 1
  private val maxErrorRange = 99

  def getErrorCode(is5xxError: Boolean) = {
    val baseCode = is5xxError match {
      case false => base4xxCode
      case true  => base5xxCode
    }
    baseCode + Gen.choose[Int](minErrorRange, maxErrorRange).sample.get
  }
}
