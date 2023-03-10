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

package uk.gov.hmrc.economiccrimelevyreturns.utils

import java.time.LocalDate
import scala.annotation.tailrec

object EclTaxYear {

  private def startYear       = LocalDate.now().getYear
  private val MonthDue        = 9
  private val DayDue          = 30
  private val EclFyEndMonth   = 3
  private val EclFyStartMonth = 4
  private val EclFyEndDay     = 31
  private val EclFyStartDay   = 1

  def dueDate: LocalDate =
    LocalDate.of(calculateYearDue(), MonthDue, DayDue)

  def yearDue: String                      = calculateYearDue().toString
  private def currentFinancialYear: String = (yearDue.toInt - 1).toString
  val YearInDays: Int                      = 365

  @tailrec
  def calculateYearDue(yearDue: Int = startYear, currentDate: LocalDate = LocalDate.now()): Int =
    if (currentDate.isAfter(LocalDate.of(yearDue, MonthDue, DayDue))) {
      calculateYearDue(yearDue + 1, currentDate)
    } else {
      yearDue
    }

  def currentFinancialYearStartDate: LocalDate =
    LocalDate.of(currentFinancialYear.toInt, EclFyStartMonth, EclFyStartDay)
  def currentFinancialYearEndDate: LocalDate   =
    LocalDate.of(currentFinancialYear.toInt + 1, EclFyEndMonth, EclFyEndDay)

  def currentFyStartYear: String = currentFinancialYearStartDate.getYear.toString
  def currentFyEndYear: String   = currentFinancialYearEndDate.getYear.toString
}
