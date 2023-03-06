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

import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase

import java.time.LocalDate

class EclTaxYearSpec extends SpecBase {

  "calculateYearDue" should {
    "return 2023 if the current date is before 30th September 2023" in {
      EclTaxYear.calculateYearDue(currentDate = LocalDate.of(2023, 9, 29))
    }

    "return 2024 if the current date is before 30th September 2024 but after 30th September 2023" in {
      EclTaxYear.calculateYearDue(currentDate = LocalDate.of(2024, 9, 29))
    }

    "return 2025 if the current date is before 30th September 2025 but after 30th September 2024" in {
      EclTaxYear.calculateYearDue(currentDate = LocalDate.of(2025, 9, 29))
    }

    "return 2026 if the current date is before 30th September 2025 but after 30th September 2025" in {
      EclTaxYear.calculateYearDue(currentDate = LocalDate.of(2026, 9, 29))
    }

    "return 2027 if the current date is before 30th September 2025 but after 30th September 2026" in {
      EclTaxYear.calculateYearDue(currentDate = LocalDate.of(2027, 9, 29))
    }
  }

}
