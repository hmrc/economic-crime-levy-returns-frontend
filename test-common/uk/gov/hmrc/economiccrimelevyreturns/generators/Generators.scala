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

package uk.gov.hmrc.economiccrimelevyreturns.generators

import org.scalacheck.Arbitrary._
import org.scalacheck.Gen._
import org.scalacheck.{Gen, Shrink}
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues
import wolfendale.scalacheck.regexp.RegexpGen

import java.time.{Instant, LocalDate, ZoneOffset}
import scala.math.BigDecimal.RoundingMode

trait Generators {

  implicit val dontShrink: Shrink[String] = Shrink.shrinkAny

  def genIntersperseString(gen: Gen[String], value: String, frequencyV: Int = 1, frequencyN: Int = 10): Gen[String] = {

    val genValue: Gen[Option[String]] = Gen.frequency(frequencyN -> None, frequencyV -> Gen.const(Some(value)))

    for {
      seq1 <- gen
      seq2 <- Gen.listOfN(seq1.length, genValue)
    } yield seq1.toSeq.zip(seq2).foldLeft("") {
      case (acc, (n, Some(v))) =>
        acc + n + v
      case (acc, (n, _))       =>
        acc + n
    }
  }

  def intsInRangeWithCommas(min: Int, max: Int): Gen[String] = {
    val numberGen = choose[Int](min, max).map(_.toString)
    genIntersperseString(numberGen, ",")
  }

  def longsInRange(min: Long, max: Long): Gen[Long] =
    Gen.chooseNum[Long](min, max)

  def longsInRangeWithCommas(min: Long, max: Long): Gen[String] = {
    val numberGen = longsInRange(min, max).map(_.toString)
    genIntersperseString(numberGen, ",")
  }

  def bigDecimalInRange(min: Double, max: Double): Gen[BigDecimal] =
    Gen.chooseNum[Double](min, max).map(BigDecimal.apply(_).setScale(2, RoundingMode.DOWN))

  def bigDecimalOutOfRange(min: BigDecimal, max: BigDecimal): Gen[BigDecimal] =
    arbitrary[BigDecimal].map(_.setScale(2, RoundingMode.DOWN)) suchThat (x => x < min || x > max)

  def bigDecimalInRangeWithCommas(min: Double, max: Double): Gen[String] = {
    val numberGen = bigDecimalInRange(min, max).map(_.toString)
    genIntersperseString(numberGen, ",")
  }

  def intsLargerThanMaxValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x > Int.MaxValue)

  def intsSmallerThanMinValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x < Int.MinValue)

  def nonNumerics: Gen[String] =
    alphaStr suchThat (_.nonEmpty)

  def decimals: Gen[String] =
    arbitrary[BigDecimal]
      .map(_.abs)
      .suchThat(_ < Int.MaxValue)
      .suchThat(!_.isValidInt)
      .map("%f".format(_))

  def currencyFormattedValue: Gen[String] =
    for {
      formattedValue <- bigDecimalInRangeWithCommas(MinMaxValues.revenueMin.toDouble, MinMaxValues.revenueMax.toDouble)
    } yield s"£$formattedValue"

  def intsBelowValue(value: Int): Gen[Int] =
    arbitrary[Int] suchThat (_ < value)

  def intsAboveValue(value: Int): Gen[Int] =
    arbitrary[Int] suchThat (_ > value)

  def intsOutsideRange(min: Int, max: Int): Gen[Int] =
    arbitrary[Int] suchThat (x => x < min || x > max)

  def longsOutsideRange(min: Long, max: Long): Gen[Long] =
    arbitrary[Long] suchThat (x => x < min || x > max)

  def doublesOutsideRange(min: Double, max: Double): Gen[Double] =
    arbitrary[Double] suchThat (x => x < min || x > max)

  def nonBooleans: Gen[String] =
    arbitrary[String]
      .suchThat(_.nonEmpty)
      .suchThat(_ != "true")
      .suchThat(_ != "false")

  def nonEmptyString: Gen[String] =
    arbitrary[String] suchThat (_.nonEmpty)

  def stringsWithMaxLength(maxLength: Int): Gen[String] =
    for {
      length <- choose(1, maxLength)
      chars  <- listOfN(length, arbitrary[Char])
    } yield chars.mkString

  def alphaNumStringsWithMaxLength(maxLength: Int): Gen[String] =
    for {
      length <- choose(1, maxLength)
      chars  <- listOfN(length, Gen.alphaNumChar)
    } yield chars.mkString

  def stringsLongerThan(minLength: Int): Gen[String] = for {
    maxLength <- (minLength * 2).max(100)
    length    <- Gen.chooseNum(minLength + 1, maxLength)
    chars     <- listOfN(length, Gen.alphaNumChar)
  } yield chars.mkString

  def stringsExceptSpecificValues(excluded: Seq[String]): Gen[String] =
    nonEmptyString suchThat (!excluded.contains(_))

  def oneOf[T](xs: Seq[Gen[T]]): Gen[T] =
    if (xs.isEmpty) {
      throw new IllegalArgumentException("oneOf called on empty collection")
    } else {
      val vector = xs.toVector
      choose(0, vector.size - 1).flatMap(vector(_))
    }

  def datesBetween(min: LocalDate, max: LocalDate): Gen[LocalDate] = {

    def toMillis(date: LocalDate): Long =
      date.atStartOfDay.atZone(ZoneOffset.UTC).toInstant.toEpochMilli

    Gen.choose(toMillis(min), toMillis(max)).map { millis =>
      Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate
    }
  }

  def stringFromRegex(maxLength: Int, regex: String): Gen[String] =
    RegexpGen.from(s"$regex").retryUntil(s => s.length <= maxLength && s.trim.nonEmpty)

  def emailAddress(maxLength: Int): Gen[String] = {
    val emailPartsLength = maxLength / 5

    for {
      firstPart  <- alphaNumStringsWithMaxLength(emailPartsLength)
      secondPart <- alphaNumStringsWithMaxLength(emailPartsLength)
      thirdPart  <- alphaNumStringsWithMaxLength(emailPartsLength)
    } yield s"$firstPart@$secondPart.$thirdPart"
  }

  def emailAddressMoreThanMaxLength(maxLength: Int): Gen[String] = {
    val moreThanMax = maxLength * 10
    for {
      firstPart  <- alphaNumStringsWithMaxLength(moreThanMax)
      secondPart <- alphaNumStringsWithMaxLength(moreThanMax)
      thirdPart  <- alphaNumStringsWithMaxLength(moreThanMax)
    } yield s"$firstPart@$secondPart.$thirdPart"
  }

  def contactNumberMoreThanMaxLength(maxLength: Int): Gen[String] = {
    val moreThanMax = maxLength
    for {
      contactNumber <- stringsLongerThan(moreThanMax)
    } yield contactNumber
  }

}
