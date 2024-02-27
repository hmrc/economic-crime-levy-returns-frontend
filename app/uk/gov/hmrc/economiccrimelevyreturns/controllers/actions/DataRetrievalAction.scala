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

import cats.data.EitherT
import play.api.mvc.{ActionTransformer, Session}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.{AuthorisedRequest, ReturnDataRequest}
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, ReturnType, SessionKeys}
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
    with FrontendHeaderCarrierProvider
    with ErrorHandler {

  override protected def transform[A](request: AuthorisedRequest[A]): Future[ReturnDataRequest[A]] =
    (for {
      eclReturn     <- eclReturnService.getOrCreateReturn(request.internalId)(hc(request), request).asResponseError
      startAmendUrl <- getStartAmendUrl(eclReturn.returnType, request.session, request.internalId)(hc(request))
      periodKey     <- getPeriodKey(request.session, request.internalId)(hc(request))
    } yield (eclReturn, startAmendUrl, periodKey)).foldF(
      error => {
        val e = error
        Future.failed(new Exception(error.message))
      },
      tuple =>
        Future.successful(
          ReturnDataRequest(
            request.request,
            request.internalId,
            tuple._1,
            tuple._2,
            request.eclRegistrationReference,
            tuple._3
          )
        )
    )

  private def getStartAmendUrl(returnType: Option[ReturnType], session: Session, internalId: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, ResponseError, Option[String]] =
    returnType match {
      case Some(AmendReturn) =>
        sessionService
          .getOptional(session, internalId, SessionKeys.StartAmendUrl)
          .asResponseError

      case _ =>
        EitherT.rightT(None)
    }

  private def getPeriodKey(session: Session, internalId: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, ResponseError, Option[String]] =
    sessionService
      .get(session, internalId, SessionKeys.PeriodKey)
      .asResponseError
      .map(Some(_))
}

trait DataRetrievalAction extends ActionTransformer[AuthorisedRequest, ReturnDataRequest]
