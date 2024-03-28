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

package uk.gov.hmrc.economiccrimelevyreturns.utils

import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase

class CorrelationIdHelperSpec extends SpecBase {

  "getOrCreateCorrelationId" should {
    "return the correlation id if there is one on the request" in forAll { id: String =>
      val result  =
        CorrelationIdHelper.getOrCreateCorrelationId(fakeRequest.withHeaders(HttpHeader.xCorrelationId -> id))
      val headers = result.headers(Seq(HttpHeader.xCorrelationId))
      headers.size    shouldBe 1
      headers.head._2 shouldBe id
    }

    "return a default correlation id if there is none on the request" in {
      val result  = CorrelationIdHelper.getOrCreateCorrelationId(fakeRequest)
      val headers = result.extraHeaders
      headers.size            shouldBe 1
      headers.head._1         shouldBe HttpHeader.xCorrelationId
      headers.head._2.isEmpty shouldBe false
    }
  }
}
