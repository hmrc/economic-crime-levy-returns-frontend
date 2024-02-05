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

import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.{AuthorisedRequest, ReturnDataRequest}

import scala.concurrent.{ExecutionContext, Future}

class FakeDataRetrievalAction(data: EclReturn, periodKey: Option[String] = None) extends DataRetrievalAction {

  override protected def transform[A](request: AuthorisedRequest[A]): Future[ReturnDataRequest[A]] =
    Future(
      ReturnDataRequest(request.request, request.internalId, data, None, request.eclRegistrationReference, periodKey)
    )

  override protected implicit val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

}
