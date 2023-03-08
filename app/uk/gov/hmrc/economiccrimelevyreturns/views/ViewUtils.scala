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

package uk.gov.hmrc.economiccrimelevyreturns.views

import play.api.data.Form
import play.api.i18n.Messages

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.{Instant, LocalDate, ZoneId}
import java.util.Date

object ViewUtils {

  def titleWithForm(form: Form[_], pageTitle: String, section: Option[String] = None)(implicit
    messages: Messages
  ): String =
    title(
      pageTitle = s"${errorPrefix(form)}${messages(pageTitle)}",
      section = section
    )

  def title(pageTitle: String, section: Option[String] = None)(implicit messages: Messages): String =
    s"${messages(pageTitle)} - ${section.fold("")(messages(_) + " - ")}${messages("service.name")} - ${messages("site.govuk")}"

  private def errorPrefix(form: Form[_])(implicit messages: Messages): String =
    if (form.hasErrors || form.hasGlobalErrors) s"${messages("error.browser.title.prefix")} " else ""

  def formatLocalDate(localDate: LocalDate, translate: Boolean = true)(implicit messages: Messages): String =
    if (translate) {
      val day   = localDate.getDayOfMonth
      val month = messages(s"date.month.${localDate.getMonthValue}")
      val year  = localDate.getYear

      s"$day $month $year"
    } else {
      val formatter = new SimpleDateFormat("d MMMM yyyy")
      formatter.format(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant))
    }

  def formatInstantAsLocalDate(instant: Instant, translate: Boolean = true)(implicit messages: Messages): String =
    formatLocalDate(LocalDate.ofInstant(instant, ZoneId.systemDefault()), translate)

  def formatMoney(amount: Number): String = {
    val formatter = NumberFormat.getCurrencyInstance
    formatter.format(amount)
  }

  def formatNumber(amount: Number): String = {
    val formatter = NumberFormat.getNumberInstance
    formatter.format(amount)
  }

}
