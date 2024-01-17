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

package uk.gov.hmrc.economiccrimelevyreturns.services

import cats.data.EitherT
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.economiccrimelevyreturns.connectors.ReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, FirstTimeReturn, ReturnType, SubmitEclReturnResponse}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, DataValidationError}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ReturnsService @Inject() (
  eclReturnsConnector: ReturnsConnector,
  auditService: AuditService
)(implicit
  ec: ExecutionContext
) extends BaseService {

  def getReturn(
    internalId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, DataHandlingError, Option[EclReturn]] =
    EitherT {
      eclReturnsConnector.getReturn(internalId).map(eclReturn => Right(Some(eclReturn))).recover {
        case UpstreamErrorResponse(_, NOT_FOUND, _, _) => Right(None)
        case error @ UpstreamErrorResponse(message, code, _, _)
            if UpstreamErrorResponse.Upstream5xxResponse
              .unapply(error)
              .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
          Left(DataHandlingError.BadGateway(reason = message, code = code))
        case NonFatal(thr)                             => Left(DataHandlingError.InternalUnexpectedError(Some(thr)))
      }
    }

  def getOrCreateReturn(
    internalId: String,
    returnType: Option[ReturnType] = None
  )(implicit hc: HeaderCarrier, request: AuthorisedRequest[_]): EitherT[Future, DataHandlingError, EclReturn] =
    getReturn(internalId)
      .flatMap {
        case Some(eclReturn) => EitherT[Future, DataHandlingError, EclReturn](Future.successful(Right(eclReturn)))
        case None            =>
          auditService
            .auditReturnStarted(
              internalId,
              request.eclRegistrationReference,
              returnType
            )

          val newReturn = EclReturn.empty(internalId, Some(returnType.getOrElse(FirstTimeReturn)))
          upsertReturn(newReturn).map(_ => newReturn)
      }

  def submitReturn(
    internalId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, DataHandlingError, SubmitEclReturnResponse] =
    EitherT {
      eclReturnsConnector
        .submitReturn(internalId)
        .map {
          Right(_)
        }
        .recover {
          handleError[SubmitEclReturnResponse]
        }
    }

  def upsertReturn(eclReturn: EclReturn)(implicit hc: HeaderCarrier): EitherT[Future, DataHandlingError, Unit] =
    EitherT {
      eclReturnsConnector
        .upsertReturn(eclReturn)
        .map {
          Right(_)
        }
        .recover {
          handleError[Unit]
        }
    }

  def deleteReturn(internalId: String)(implicit hc: HeaderCarrier): EitherT[Future, DataHandlingError, Unit] =
    EitherT {
      eclReturnsConnector
        .deleteReturn(internalId)
        .map {
          Right(_)
        }
        .recover {
          handleError[Unit]
        }
    }

  def getReturnValidationErrors(
    internalId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, DataHandlingError, Option[DataValidationError]] =
    EitherT {
      eclReturnsConnector
        .validateEclReturn(internalId)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DataHandlingError.BadGateway(reason = message, code = code))
          case NonFatal(thr) => Left(DataHandlingError.InternalUnexpectedError(Some(thr)))
        }
    }

}
