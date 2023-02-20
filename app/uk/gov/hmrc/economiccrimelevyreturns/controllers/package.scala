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

package uk.gov.hmrc.economiccrimelevyreturns

import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest

package object controllers {
  val contactName: ReturnDataRequest[_] => String = request =>
    request.eclReturn.contactName
      .getOrElse(throw new IllegalStateException("No contact name found in returns data"))
}
