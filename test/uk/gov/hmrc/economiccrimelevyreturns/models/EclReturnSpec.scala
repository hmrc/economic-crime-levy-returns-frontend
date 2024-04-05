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

package uk.gov.hmrc.economiccrimelevyreturns.models

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.libs.json.{JsBoolean, JsError, JsResultException, JsString, Json}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._

class EclReturnSpec extends SpecBase {

  def returnWithNoContactInfo: EclReturn =
    returnWithContactInfo(hasName = false, hasRole = false, hasEmail = false, hasNumber = false)

  def returnWithContactInfo(hasName: Boolean, hasRole: Boolean, hasEmail: Boolean, hasNumber: Boolean): EclReturn = {
    def value(isPresent: Boolean) =
      if (isPresent) Some(random[String]) else None

    random[EclReturn].copy(
      contactName = value(hasName),
      contactRole = value(hasRole),
      contactEmailAddress = value(hasEmail),
      contactTelephoneNumber = value(hasNumber)
    )
  }

  "hasContactInfo" should {
    "return false if no contact info" in {
      returnWithNoContactInfo.hasContactInfo shouldBe false
    }

    "return true if at least ome of the contact fields is set" in forAll {
      (hasName: Boolean, hasRole: Boolean, hasEmail: Boolean, hasNumber: Boolean) =>
        if (hasName || hasRole || hasEmail || hasNumber) {
          returnWithContactInfo(hasName, hasRole, hasEmail, hasNumber).hasContactInfo shouldBe true
        }
    }
  }

  "writes" should {
    "return the return type serialized to its JSON representation" in forAll { (returnType: ReturnType) =>
      val result = Json.toJson(returnType)

      result shouldBe JsString(returnType.toString)
    }
  }

  "reads" should {
    "return the return type deserialized from its JSON representation" in forAll { (returnType: ReturnType) =>
      val json = Json.toJson(returnType)

      json.as[ReturnType] shouldBe returnType
    }

    "return error when trying to deserialize unknown return type" in forAll { (returnType: ReturnType) =>
      val json = Json.toJson(100)

      val error = intercept[JsResultException] {
        json.as[ReturnType] shouldBe returnType
      }

      assert(error.errors.nonEmpty)
    }

    "return a JsError when passed a type that is not a string" in {
      val result = Json.fromJson[ReturnType](JsBoolean(true))

      result shouldBe a[JsError]
    }
  }

}
