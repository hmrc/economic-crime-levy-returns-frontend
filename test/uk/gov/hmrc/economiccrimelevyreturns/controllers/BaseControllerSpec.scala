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

package uk.gov.hmrc.economiccrimelevyreturns.controllers

import play.api.i18n.MessagesApi
import play.api.mvc.AnyContentAsEmpty
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest

class BaseControllerSpec extends SpecBase {

  val controller: BaseController = new BaseController {
    override def messagesApi: MessagesApi = null
  }

  val text = "Value"

  def getReturnRequest(eclReturn: EclReturn, name: Option[String]): ReturnDataRequest[AnyContentAsEmpty.type] =
    ReturnDataRequest(
      fakeRequest,
      eclReturn.internalId,
      eclReturn.copy(contactName = name),
      None,
      eclReturn.internalId,
      None
    )

  "valueOrErrorF" should {
    "return a value when there is one" in forAll { value: String =>
      val result = await(controller.valueOrErrorF(Some(value), text).value)
      result shouldBe Right(value)
    }

    "return an when there is no data" in {
      val result = await(controller.valueOrErrorF(None, text).value)
      result shouldBe Left(ResponseError.internalServiceError(s"Missing $text"))
    }
  }

  "valueOrError" should {
    "return a value when there is one" in forAll { value: String =>
      val result = controller.valueOrError(Some(value), text)
      result shouldBe Right(value)
    }

    "return an when there is no data" in {
      val result = controller.valueOrError(None, text)
      result shouldBe Left(ResponseError.internalServiceError(s"Missing $text"))
    }
  }

  "getContactNameFromRequest" should {
    "return the contact name when there is one" in forAll { (eclReturn: EclReturn, name: String) =>
      val request = getReturnRequest(eclReturn, Some(name))
      val result  = controller.getContactNameFromRequest(request)
      result shouldBe Right(name)
    }

    "return an error when there is no contact name" in forAll { eclReturn: EclReturn =>
      val request = getReturnRequest(eclReturn, None)
      val result  = controller.getContactNameFromRequest(request)
      result shouldBe Left(ResponseError.internalServiceError())
    }
  }

  "addToSession" should {
    "add data to the request session" in forAll { string: String =>
      fakeRequest.session.get(string) shouldBe None
      val data   = Seq((string, string))
      val result = controller.addToSession(data)(fakeRequest)
      result.get(string) shouldBe Some(string)
    }

    "add data to a given session" in forAll { string: String =>
      val session = fakeRequest.session
      session.get(string) shouldBe None
      val data   = Seq((string, string))
      val result = controller.addToSession(session, data)
      result.get(string) shouldBe Some(string)
    }
  }
}
