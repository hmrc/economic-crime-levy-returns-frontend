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

import cats.data.EitherT
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyreturns.models.{FirstTimeReturn, SessionData, SessionKeys}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.SessionError
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest
import uk.gov.hmrc.economiccrimelevyreturns.services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StoreUrlAction @Inject() (
  sessionService: SessionService
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[ReturnDataRequest, ReturnDataRequest]
    with FrontendHeaderCarrierProvider
    with ErrorHandler {

  override protected def refine[A](
    request: ReturnDataRequest[A]
  ): Future[Either[Result, ReturnDataRequest[A]]] = {
    implicit val hcFromRequest: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val sessionData = SessionData(request.internalId, Map(SessionKeys.urlToReturnTo -> request.uri))

    (for {
      _ <- request.eclReturn.returnType match {
             case Some(FirstTimeReturn) => sessionService.upsert(sessionData).asResponseError
             case _                     => EitherT[Future, SessionError, Unit](Future.successful(Right(()))).asResponseError
           }
    } yield ()).foldF(
      error => Future.failed(new Exception(error.message)),
      _ => Future.successful(Right(request))
    )
  }
}
