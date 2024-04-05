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
import play.api.mvc.Session
import uk.gov.hmrc.economiccrimelevyreturns.connectors.SessionDataConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionData
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.SessionError
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class SessionService @Inject() (sessionRetrievalConnector: SessionDataConnector)(implicit ec: ExecutionContext) {

  def get(session: Session, internalId: String, key: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, SessionError, String] =
    Try {
      session(key)
    } match {
      case Success(value) => EitherT.rightT(value)
      case Failure(_)     =>
        for {
          sessionDataOpt <- getSessionData(internalId)
          sessionData    <- valueOrNotFound(sessionDataOpt)
          value          <- retrieveValueFromSessionData(sessionData, key)
        } yield value
    }

  def getOptional(session: Session, internalId: String, key: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, SessionError, Option[String]] =
    EitherT {
      get(session, internalId, key)
        .fold(
          {
            case _: SessionError.KeyNotFound | _: SessionError.NotFound => Right(None)
            case error                                                  => Left(error)
          },
          value => Right(Some(value))
        )
    }

  private def valueOrNotFound(sessionDataOpt: Option[SessionData]): EitherT[Future, SessionError, SessionData] =
    EitherT {
      sessionDataOpt match {
        case Some(sessionData) => Future.successful(Right(sessionData))
        case None              => Future.successful(Left(SessionError.NotFound()))
      }
    }

  def upsert(sessionData: SessionData)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, SessionError, Unit] =
    for {
      oldSessionData <- getSessionData(sessionData.internalId)
      newSessionData  = mergeSessionData(oldSessionData, sessionData)
      _              <- upsertSessions(newSessionData)
    } yield ()

  def delete(
    internalId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, SessionError, Unit] =
    EitherT {
      sessionRetrievalConnector
        .delete(internalId)
        .map {
          Right(_)
        }
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(SessionError.BadGateway(message, code))
          case NonFatal(thr) =>
            Left(SessionError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  private def getSessionData(
    internalId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, SessionError, Option[SessionData]] =
    EitherT {
      sessionRetrievalConnector
        .get(internalId)
        .map(s => Right(Some(s)))
        .recover {
          case _ @UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
            Right(None)
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(SessionError.BadGateway(message, code))
          case NonFatal(thr)                                =>
            Left(SessionError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  private def retrieveValueFromSessionData(
    sessionData: SessionData,
    key: String
  ): EitherT[Future, SessionError, String] =
    EitherT {
      sessionData.values.get(key) match {
        case Some(value) => Future.successful(Right(value))
        case None        =>
          Future.successful(Left(SessionError.KeyNotFound(key)))
      }
    }

  private def upsertSessions(sessionData: SessionData)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, SessionError, Unit] =
    EitherT {
      sessionRetrievalConnector
        .upsert(sessionData)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse.unapply(error).isDefined ||
                UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(SessionError.BadGateway(message, code))
          case NonFatal(thr) => Left(SessionError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  private def mergeSessionData(oldSessionData: Option[SessionData], newSessionData: SessionData): SessionData =
    oldSessionData
      .map(oldData =>
        newSessionData.copy(
          values = newSessionData.values ++ oldData.values.filterNot(e => newSessionData.values.keySet.contains(e._1))
        )
      )
      .getOrElse(newSessionData)
}
