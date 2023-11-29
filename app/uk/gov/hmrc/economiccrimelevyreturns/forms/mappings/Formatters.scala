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

import play.api.data.FormError
import play.api.data.format.Formatter
import uk.gov.hmrc.economiccrimelevyreturns.models.Enumerable

import scala.util.control.Exception.nonFatalCatch

trait Formatters {

  private lazy val validRevenuePattern = """^(\d*(\.[0-9]{1,2})?)$""".r
  private lazy val decimalRegexp       = """^(\d*\.\d*)$""".r

  private def removeWhitespace(value: String, removeAllWhitespace: Boolean) =
    if (removeAllWhitespace) value.filterNot(_.isWhitespace) else value.strip()

  private def removePoundSign(value: String) =
    if (value.startsWith("£")) value.replaceFirst("£", "") else value

  private def removeCommas(value: String) =
    value.filterNot(_ == ',')

  private[mappings] def stringFormatter(requiredErrorKey: String, removeAllWhitespace: Boolean): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
        data.get(key).map(s => removeWhitespace(s, removeAllWhitespace)) match {
          case None        => Left(Seq(FormError(key, requiredErrorKey)))
          case Some(value) =>
            if (value.isEmpty) Left(Seq(FormError(key, requiredErrorKey))) else Right(value)
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)
    }

  private[mappings] def booleanFormatter(
    requiredKey: String,
    invalidKey: String
  ): Formatter[Boolean] =
    new Formatter[Boolean] {

      private val baseFormatter = stringFormatter(requiredKey, removeAllWhitespace = true)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .flatMap {
            case "true"  => Right(true)
            case "false" => Right(false)
            case _       => Left(Seq(FormError(key, invalidKey)))
          }

      def unbind(key: String, value: Boolean) = Map(key -> value.toString)
    }

  def currencyFormatter(
    requiredKey: String,
    nonCurrencyKey: String
  ): Formatter[BigDecimal] =
    new Formatter[BigDecimal] {

      private val baseFormatter = stringFormatter(requiredKey, removeAllWhitespace = true)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .map(removeCommas)
          .map(removePoundSign)
          .flatMap { value =>
            if (validRevenuePattern.matches(value)) {
              Right(value.toDouble)
            } else {
              Left(Seq(FormError(key, nonCurrencyKey)))
            }
          }

      override def unbind(key: String, value: BigDecimal) =
        baseFormatter.unbind(key, value.toString)
    }

  private def numberFormatter[T](
    stringToNumber: String => T,
    requiredKey: String,
    wholeNumberKey: String,
    nonNumericKey: String
  ): Formatter[T] =
    new Formatter[T] {

      private val baseFormatter = stringFormatter(requiredKey, removeAllWhitespace = true)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .map(removeCommas)
          .flatMap {
            case decimalRegexp(_) =>
              Left(Seq(FormError(key, wholeNumberKey)))
            case number           =>
              nonFatalCatch
                .either(stringToNumber(number))
                .left
                .map(_ => Seq(FormError(key, nonNumericKey)))
          }

      override def unbind(key: String, value: T) =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def longFormatter(
    requiredKey: String,
    wholeNumberKey: String,
    nonNumericKey: String
  ): Formatter[Long] = numberFormatter[Long](_.toLong, requiredKey, wholeNumberKey, nonNumericKey)

  private[mappings] def intFormatter(
    requiredKey: String,
    wholeNumberKey: String,
    nonNumericKey: String
  ): Formatter[Int] = numberFormatter[Int](_.toInt, requiredKey, wholeNumberKey, nonNumericKey)

  private[mappings] def enumerableFormatter[A](requiredKey: String, invalidKey: String)(implicit
    ev: Enumerable[A]
  ): Formatter[A] =
    new Formatter[A] {

      private val baseFormatter = stringFormatter(requiredKey, removeAllWhitespace = true)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], A] =
        baseFormatter.bind(key, data).flatMap { str =>
          ev.value(str)
            .map(Right.apply)
            .getOrElse(Left(Seq(FormError(key, invalidKey))))
        }

      override def unbind(key: String, value: A): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }
}
