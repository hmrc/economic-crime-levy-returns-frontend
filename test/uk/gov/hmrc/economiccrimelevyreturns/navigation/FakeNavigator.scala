/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyreturns.navigation

import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyreturns.models.{Mode, Return}
import uk.gov.hmrc.economiccrimelevyreturns.pages.Page

class FakeNavigator(desiredRoute: Call) extends Navigator {

  override def nextPage(page: Page, mode: Mode, eclReturn: Return): Call =
    desiredRoute
}
