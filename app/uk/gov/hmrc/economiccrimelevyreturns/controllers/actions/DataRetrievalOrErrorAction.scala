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
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result, Session}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.{ErrorHandler, routes}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, ErrorCode, ResponseError}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.{AuthorisedRequest, ReturnDataRequest}
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, EclReturn, FirstTimeReturn, Fulfilled, ObligationData, ObligationDetails, Open, ReturnType, SessionKeys}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclAccountService, ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdHelper
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnDataRetrievalOrErrorAction @Inject() (
  eclReturnService: ReturnsService,
  sessionService: SessionService,
  eclAccountService: EclAccountService
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalOrErrorAction
    with FrontendHeaderCarrierProvider
    with ErrorHandler {

  override protected def refine[A](request: AuthorisedRequest[A]): Future[Either[Result, ReturnDataRequest[A]]] =
    (for {
      eclReturnOption <- eclReturnService.getReturn(request.internalId)(hc(request)).asResponseError
      eclReturn       <- extractReturn(eclReturnOption)
      startAmendUrl   <- getStartAmendUrl(eclReturn.returnType, request.session, request.internalId)(hc(request))
      periodKey       <- getPeriodKey(request.session, request.internalId)(hc(request), request)
      updatedReturn   <- addObligationDetails(eclReturn, periodKey)(request)
    } yield (updatedReturn, startAmendUrl, periodKey)).foldF(
      error =>
        error.code match {
          case ErrorCode.NotFound =>
            Future.successful(Left(Redirect(getRedirectUrl(request))))
          case _                  => Future.failed(new Exception(error.message))
        },
      tuple => {
        val eclReturn     = tuple._1
        val startAmendUrl = tuple._2
        val periodKey     = tuple._3

        Future.successful(
          Right(
            ReturnDataRequest(
              request.request,
              request.internalId,
              eclReturn,
              startAmendUrl,
              request.eclRegistrationReference,
              Some(periodKey)
            )
          )
        )
      }
    )

  private def getRedirectUrl(request: AuthorisedRequest[_]) =
    if (request.uri.contains("FirstTimeReturn")) {
      routes.NotableErrorController.eclReturnAlreadySubmitted()
    } else {
      routes.NotableErrorController.returnAmendmentAlreadyRequested()
    }
  private def addObligationDetails(eclReturn: EclReturn, periodKey: String)(implicit
    request: AuthorisedRequest[_]
  ): EitherT[Future, ResponseError, EclReturn]              =
    if (eclReturn.obligationDetails.isEmpty) {
      for {
        obligationData <- eclAccountService.retrieveObligationData.asResponseError
        updatedReturn  <- processObligationDetails(eclReturn, obligationData, periodKey).asResponseError
      } yield updatedReturn
    } else { EitherT.fromEither[Future](Right(eclReturn)) }

  private def processObligationDetails(eclReturn: EclReturn, obligationData: Option[ObligationData], periodKey: String)(
    implicit request: AuthorisedRequest[_]
  ): EitherT[Future, DataHandlingError, EclReturn] = {
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)

    val obligationDetails =
      obligationData.flatMap(_.getObligationDetails(periodKey))

    obligationDetails match {
      case None                    =>
        EitherT.fromEither[Future](Right(eclReturn))
      case Some(obligationDetails) =>
        obligationDetails.status match {
          case Fulfilled =>
            if (obligationDetails.inboundCorrespondenceDateReceived.isEmpty) {
              EitherT.fromEither[Future](
                Left(
                  DataHandlingError.InternalUnexpectedError(
                    None,
                    Some("Fulfilled obligation does not have an inboundCorrespondenceDateReceived")
                  )
                )
              )
            } else {
              EitherT.fromEither[Future](Right(eclReturn))
            }
          case Open      =>
            for {
              eclReturn     <- eclReturnService.getOrCreateReturn(request.internalId, Some(FirstTimeReturn))
              updatedReturn <- upsertObligationDetails(eclReturn, obligationDetails, periodKey)
            } yield updatedReturn
        }
    }
  }

  private def upsertObligationDetails(eclReturn: EclReturn, obligationDetails: ObligationDetails, periodKey: String)(
    implicit request: AuthorisedRequest[_]
  ): EitherT[Future, DataHandlingError, EclReturn] = {
    val optPeriodKey = eclReturn.obligationDetails.map(_.periodKey)
    if (optPeriodKey.contains(periodKey) || optPeriodKey.isEmpty) {
      val updatedReturn =
        eclReturn.copy(obligationDetails = Some(obligationDetails), returnType = Some(FirstTimeReturn))
      eclReturnService.upsertReturn(updatedReturn)
      EitherT.fromEither[Future](Right(updatedReturn))
    } else {
      for {
        _            <- eclReturnService.deleteReturn(request.internalId)
        updatedReturn = EclReturn
                          .empty(request.internalId, None)
                          .copy(obligationDetails = Some(obligationDetails), returnType = Some(FirstTimeReturn))
        _            <- eclReturnService.upsertReturn(updatedReturn)
      } yield updatedReturn
    }
  }

  private def getStartAmendUrl(returnType: Option[ReturnType], session: Session, internalId: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, ResponseError, Option[String]] =
    returnType match {
      case Some(AmendReturn) =>
        sessionService
          .getOptional(session, internalId, SessionKeys.startAmendUrl)
          .asResponseError
      case _                 =>
        EitherT.rightT(None)
    }

  private def getPeriodKey(session: Session, internalId: String)(implicit
    hc: HeaderCarrier,
    request: AuthorisedRequest[_]
  ): EitherT[Future, ResponseError, String] = {
    val periodKeyUri = request.uri.split("/period/")

    if (periodKeyUri.isDefinedAt(1)) {
      EitherT.fromEither[Future](Right(periodKeyUri(1)))
    } else {
      sessionService
        .get(session, internalId, SessionKeys.periodKey)
        .asResponseError
    }
  }

  private def extractReturn(returnOption: Option[EclReturn]) =
    EitherT {
      Future.successful(
        returnOption match {
          case Some(eclReturn) => Right(eclReturn)
          case None            => Left(ResponseError.notFoundError("Failed to find return"))
        }
      )
    }

}

trait DataRetrievalOrErrorAction extends ActionRefiner[AuthorisedRequest, ReturnDataRequest]
