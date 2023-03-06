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

package uk.gov.hmrc.economiccrimelevyreturns.controllers.actions

import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest

import scala.concurrent.{ExecutionContext, Future}

class FakeValidatedReturnAction(data: EclReturn) extends ValidatedReturnAction {
  override protected def filter[A](request: ReturnDataRequest[A]): Future[Option[Result]] =
    Future.successful(None)

  override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}
