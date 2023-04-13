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
import play.api.data.Forms.{single, text}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.{ObligationDetails, Open}

import java.time.{Instant, LocalDate}

class ViewUtilsSpec extends SpecBase {

  val testForm: Form[String] = Form(
    single("testValue" -> text)
  )

  val testTitle: String = "Test Title"

  "titleWithForm" should {
    "return a correctly formatted title when a form has no errors" in {
      ViewUtils.titleWithForm(testForm, testTitle, None)(
        messages
      ) shouldBe "Test Title - Submit your Economic Crime Levy return - GOV.UK"
    }

    "return a correctly formatted title when a form has errors" in {
      ViewUtils.titleWithForm(testForm.withError(key = "testErrorKey", message = "testErrorMessage"), testTitle, None)(
        messages
      ) shouldBe "Error: Test Title - Submit your Economic Crime Levy return - GOV.UK"
    }
  }

  "title" should {
    "return a correctly formatted title when there is no section" in {
      ViewUtils.title(testTitle, None)(
        messages
      ) shouldBe "Test Title - Submit your Economic Crime Levy return - GOV.UK"
    }

    "return a correctly formatted title when there is a section" in {
      ViewUtils.title(testTitle, Some("Test Section"))(
        messages
      ) shouldBe "Test Title - Test Section - Submit your Economic Crime Levy return - GOV.UK"
    }
  }

  "formatLocalDate" should {
    "correctly format a translated local date" in {
      val localDate = LocalDate.parse("2007-12-03")

      ViewUtils.formatLocalDate(localDate)(messages) shouldBe "3 December 2007"
    }

    "correctly format a non-translated local date" in {
      val localDate = LocalDate.parse("2007-12-03")

      ViewUtils.formatLocalDate(localDate, translate = false)(messages) shouldBe "3 December 2007"
    }
  }

  "formatInstantAsLocalDate" should {
    "correctly format a translated instant" in {
      val instant = Instant.parse("2007-12-03T10:04:39.528Z")

      ViewUtils.formatInstantAsLocalDate(instant)(messages) shouldBe "3 December 2007"
    }

    "correctly format a non-translated instant" in {
      val instant = Instant.parse("2007-12-03T10:04:39.528Z")

      ViewUtils.formatInstantAsLocalDate(instant, translate = false)(messages) shouldBe "3 December 2007"
    }
  }

  "formatMoney" should {
    "correctly format a monetary amount that includes pence" in {
      ViewUtils.formatMoney(123456789.5) shouldBe "£123,456,789.50"
    }

    "correctly format a monetary amount that does not include pence" in {
      ViewUtils.formatMoney(123456789.0) shouldBe "£123,456,789"
    }
  }

  "formatObligationPeriodYears" should {
    "correctly format the start and end year of an obligation period" in {
      ViewUtils.formatObligationPeriodYears(
        ObligationDetails(
          status = Open,
          inboundCorrespondenceFromDate = LocalDate.parse("2022-04-01"),
          inboundCorrespondenceToDate = LocalDate.parse("2023-03-31"),
          inboundCorrespondenceDateReceived = None,
          inboundCorrespondenceDueDate = LocalDate.parse("2023-09-30"),
          periodKey = "22XY"
        )
      ) shouldBe "2022-2023"
    }

  }

}
