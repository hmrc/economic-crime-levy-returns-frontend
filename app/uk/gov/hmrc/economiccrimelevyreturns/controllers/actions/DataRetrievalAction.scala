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

import play.api.mvc.{ActionTransformer, Session}
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, FirstTimeReturn, ReturnType, SessionKeys}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.{AuthorisedRequest, ReturnDataRequest}
import uk.gov.hmrc.economiccrimelevyreturns.services.{ReturnsService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnDataRetrievalAction @Inject() (
  val eclReturnService: ReturnsService,
  val sessionService: SessionService
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction
    with FrontendHeaderCarrierProvider {

  override protected def transform[A](request: AuthorisedRequest[A]): Future[ReturnDataRequest[A]] =
    eclReturnService.getOrCreateReturn(request.internalId)(hc(request), request).flatMap { eclReturn =>
      getStartAmendUrl(eclReturn.returnType, request.session, request.internalId)(hc(request)).map { startAmendUrl =>
        ReturnDataRequest(
          request.request,
          request.internalId,
          eclReturn,
          startAmendUrl,
          request.eclRegistrationReference
        )
      }
    }

  private def getStartAmendUrl(returnType: Option[ReturnType], session: Session, internalId: String)(implicit
    hc: HeaderCarrier
  ): Future[Option[String]] =
    returnType match {
      case Some(AmendReturn) =>
        sessionService.get(session, internalId, SessionKeys.StartAmendUrl)
      case _                 =>
        Future.successful(None)
    }
}

trait DataRetrievalAction extends ActionTransformer[AuthorisedRequest, ReturnDataRequest]
