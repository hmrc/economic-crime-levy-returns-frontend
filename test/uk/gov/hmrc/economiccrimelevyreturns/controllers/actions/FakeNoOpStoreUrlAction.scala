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

package uk.gov.hmrc.economiccrimelevyreturns.controllers.actions

import org.mockito.MockitoSugar.mock
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest
import uk.gov.hmrc.economiccrimelevyreturns.services.SessionService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeNoOpStoreUrlAction @Inject() ()(implicit
  override val executionContext: ExecutionContext
) extends StoreUrlAction(mock[SessionService]) {

  override protected def refine[A](
    request: ReturnDataRequest[A]
  ): Future[Either[Result, ReturnDataRequest[A]]] = Future.successful(Right(request))

}
