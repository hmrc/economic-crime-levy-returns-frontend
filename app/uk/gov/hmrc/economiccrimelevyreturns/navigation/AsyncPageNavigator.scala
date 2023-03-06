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

package uk.gov.hmrc.economiccrimelevyreturns.navigation

import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, Mode, NormalMode}

import scala.concurrent.Future

trait AsyncPageNavigator {
  def nextPage(mode: Mode, eclReturn: EclReturn)(implicit request: RequestHeader): Future[Call] = mode match {
    case NormalMode => navigateInNormalMode(eclReturn)
    case CheckMode  => navigateInCheckMode(eclReturn)
  }

  protected def navigateInNormalMode(eclReturn: EclReturn)(implicit request: RequestHeader): Future[Call]

  protected def navigateInCheckMode(eclReturn: EclReturn)(implicit request: RequestHeader): Future[Call]

}
