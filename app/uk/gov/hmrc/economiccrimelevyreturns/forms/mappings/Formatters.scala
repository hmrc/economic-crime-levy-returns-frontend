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

  private lazy val twoDecimalPattern = """^-?(\d*\.[0-9]{0,2})$""".r
  private lazy val decimalRegexp     = """^-?(\d*\.\d*)$""".r

  private def removeWhitespace(value: String) =
    value.trim.filterNot(_.isWhitespace)

  private def removePoundSign(value: String) =
    value.filterNot(_ == '£')

  private def removeCommas(value: String) =
    value.filterNot(_ == ',')

  private[mappings] def stringFormatter(errorKey: String, args: Seq[String] = Seq.empty): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
        data.get(key) match {
          case None                      => Left(Seq(FormError(key, errorKey, args)))
          case Some(s) if s.trim.isEmpty => Left(Seq(FormError(key, errorKey, args)))
          case Some(s)                   => Right(s)
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)
    }

  private[mappings] def booleanFormatter(
    requiredKey: String,
    invalidKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[Boolean] =
    new Formatter[Boolean] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .flatMap {
            case "true"  => Right(true)
            case "false" => Right(false)
            case _       => Left(Seq(FormError(key, invalidKey, args)))
          }

      def unbind(key: String, value: Boolean) = Map(key -> value.toString)
    }

  def currencyFormatter(
    requiredKey: String,
    nonCurrencyKey: String
  ): Formatter[Long] =
    new Formatter[Long] {

      private val baseFormatter = stringFormatter(requiredKey)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .map(removeCommas)
          .map(removePoundSign)
          .flatMap { value =>
            val number = value match {
              case twoDecimalPattern(d) =>
                d.split('.')(0)
              case d                    => d
            }

            nonFatalCatch
              .either(number.toLong)
              .left
              .map(_ => Seq(FormError(key, nonCurrencyKey)))
          }

      override def unbind(key: String, value: Long) =
        baseFormatter.unbind(key, value.toString)
    }

  private def numberFormatter[T](
    stringToNumber: String => T,
    requiredKey: String,
    wholeNumberKey: String,
    nonNumericKey: String,
    args: Seq[String]
  ): Formatter[T] =
    new Formatter[T] {

      val decimalRegexp = """^-?(\d*\.\d*)$"""

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", ""))
          .map(_.replaceAll("\\s+", ""))
          .flatMap {
            case s if s.matches(decimalRegexp) =>
              Left(Seq(FormError(key, wholeNumberKey, args)))
            case s                             =>
              nonFatalCatch
                .either(stringToNumber(s))
                .left
                .map(_ => Seq(FormError(key, nonNumericKey, args)))
          }

      override def unbind(key: String, value: T) =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def longFormatter(
    requiredKey: String,
    wholeNumberKey: String,
    nonNumericKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[Long] = numberFormatter[Long](_.toLong, requiredKey, wholeNumberKey, nonNumericKey, args)

  private[mappings] def intFormatter(
    requiredKey: String,
    wholeNumberKey: String,
    nonNumericKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[Int] = numberFormatter[Int](_.toInt, requiredKey, wholeNumberKey, nonNumericKey, args)

  private[mappings] def enumerableFormatter[A](requiredKey: String, invalidKey: String, args: Seq[String] = Seq.empty)(
    implicit ev: Enumerable[A]
  ): Formatter[A] =
    new Formatter[A] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], A] =
        baseFormatter.bind(key, data).flatMap { str =>
          ev.value(str)
            .map(Right.apply)
            .getOrElse(Left(Seq(FormError(key, invalidKey, args))))
        }

      override def unbind(key: String, value: A): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }
}
