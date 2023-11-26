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

package uk.gov.hmrc.economiccrimelevyreturns.forms.behaviours

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.{Form, FormError}
import uk.gov.hmrc.economiccrimelevyreturns.forms.FormSpec
import uk.gov.hmrc.economiccrimelevyreturns.generators.Generators

trait FieldBehaviours extends FormSpec with ScalaCheckPropertyChecks with Generators {

  private def removeCommas(value: String) =
    value.filterNot(_ == ',')

  def fieldThatBindsValidData(form: Form[_], fieldName: String, validDataGenerator: Gen[String]): Unit =
    "bind" should {
      "bind valid data" in {

        forAll(validDataGenerator -> "validDataItem") { dataItem: String =>
          val result = form.bind(Map(fieldName -> dataItem)).apply(fieldName)
          result.value.value shouldBe dataItem
          result.errors      shouldBe empty
        }
      }
    }

  def mandatoryField(form: Form[_], fieldName: String, requiredError: FormError): Unit =
    "bind" should {
      "not bind when key is not present at all" in {

        val result = form.bind(emptyForm).apply(fieldName)
        result.errors shouldEqual Seq(requiredError)
      }

      "not bind blank values" in {

        val result = form.bind(Map(fieldName -> "")).apply(fieldName)
        result.errors shouldEqual Seq(requiredError)
      }
    }
}
