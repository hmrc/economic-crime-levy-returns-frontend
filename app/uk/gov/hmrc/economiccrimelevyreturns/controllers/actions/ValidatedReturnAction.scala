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

import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidatedReturnActionImpl @Inject() (eclReturnsConnector: EclReturnsConnector)(implicit
  val executionContext: ExecutionContext
) extends ValidatedReturnAction
    with FrontendHeaderCarrierProvider {

  override def filter[A](request: ReturnDataRequest[A]): Future[Option[Result]] =
    eclReturnsConnector.getReturnValidationErrors(request.internalId)(hc(request)).map {
      case Some(_) => Some(Redirect(routes.NotableErrorController.answersAreInvalid()))
      case None    => None
    }
}

trait ValidatedReturnAction extends ActionFilter[ReturnDataRequest]
