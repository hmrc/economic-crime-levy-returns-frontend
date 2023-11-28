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

package uk.gov.hmrc.economiccrimelevyreturns.forms.mappings

import play.api.data.FieldMapping
import play.api.data.Forms.of
import uk.gov.hmrc.economiccrimelevyreturns.models.Enumerable

import java.time.LocalDate

trait Mappings extends Formatters with Constraints {

  protected def text(errorKey: String = "error.required", removeAllWhitespace: Boolean): FieldMapping[String] =
    of(stringFormatter(errorKey, removeAllWhitespace))

  protected def int(
    requiredKey: String = "error.required",
    wholeNumberKey: String = "error.wholeNumber",
    nonNumericKey: String = "error.nonNumeric"
  ): FieldMapping[Int] =
    of(intFormatter(requiredKey, wholeNumberKey, nonNumericKey))

  protected def long(
    requiredKey: String = "error.required",
    wholeNumberKey: String = "error.wholeNumber",
    nonNumericKey: String = "error.nonNumeric"
  ): FieldMapping[Long] =
    of(longFormatter(requiredKey, wholeNumberKey, nonNumericKey))

  protected def currency(
    requiredKey: String = "error.required",
    nonNumericKey: String = "error.nonNumeric"
  ): FieldMapping[Double] =
    of(currencyFormatter(requiredKey, nonNumericKey))

  protected def boolean(
    requiredKey: String = "error.required",
    invalidKey: String = "error.boolean"
  ): FieldMapping[Boolean] =
    of(booleanFormatter(requiredKey, invalidKey))

  protected def enumerable[A](
    requiredKey: String = "error.required",
    invalidKey: String = "error.invalid"
  )(implicit ev: Enumerable[A]): FieldMapping[A] =
    of(enumerableFormatter[A](requiredKey, invalidKey))

  protected def localDate(
    invalidKey: String,
    allRequiredKey: String,
    twoRequiredKey: String,
    requiredKey: String
  ): FieldMapping[LocalDate] =
    of(new LocalDateFormatter(invalidKey, allRequiredKey, twoRequiredKey, requiredKey))
}
