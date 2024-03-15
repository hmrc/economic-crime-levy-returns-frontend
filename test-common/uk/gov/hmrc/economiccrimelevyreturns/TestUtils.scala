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

package uk.gov.hmrc.economiccrimelevyreturns

import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.{MinMaxValues, Regex}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn

import scala.util.Random

trait TestUtils {
  private val digits = "1234567890"

  val blankReturn = EclReturn.empty("", None)

  def updateContactName(eclReturn: EclReturn) = {
    val name = alphaNumStringsWithMaxLength(MinMaxValues.NameMaxLength).sample.get.trim
    eclReturn.copy(contactName = Some(name))
  }

  def ensureMaxLength(string: String, max: Int): String =
    if (string.isEmpty) {
      digits.charAt(Random.nextInt(digits.length)).toString
    } else if (string.length > max) {
      string.substring(0, max)
    } else {
      string
    }
}
