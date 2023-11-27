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

import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.economiccrimelevyreturns.connectors.{AdditionalInfoConnector, EclReturnsConnector}
import uk.gov.hmrc.economiccrimelevyreturns.models.{AdditionalInfo, EclReturn, FirstTimeReturn, ReturnType}
import uk.gov.hmrc.economiccrimelevyreturns.models.audit.ReturnStartedEvent
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EclReturnsService @Inject() (
  eclReturnsConnector: EclReturnsConnector,
  additionalInfoConnector: AdditionalInfoConnector,
  auditConnector: AuditConnector
)(implicit
  ec: ExecutionContext
) {

  def getOrCreateReturn(
    internalId: String,
    returnType: Option[ReturnType] = None
  )(implicit hc: HeaderCarrier, request: AuthorisedRequest[_]): Future[EclReturn] =
    eclReturnsConnector.getReturn(internalId).recoverWith { case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
      auditConnector
        .sendExtendedEvent(
          ReturnStartedEvent(
            internalId,
            request.eclRegistrationReference,
            returnType = returnType.getOrElse(FirstTimeReturn)
          ).extendedDataEvent
        )

      returnType match {
        case None        => eclReturnsConnector.upsertReturn(EclReturn.empty(internalId, Some(FirstTimeReturn)))
        case Some(value) => eclReturnsConnector.upsertReturn(EclReturn.empty(internalId, Some(value)))
      }
    }

  def upsertEclReturnType(internalId: String, returnType: ReturnType)(implicit hc: HeaderCarrier): Future[EclReturn] =
    for {
      eclReturn        <- eclReturnsConnector.getReturn(internalId)
      updatedEclReturn <- eclReturnsConnector.upsertReturn(eclReturn.copy(returnType = Some(returnType)))
    } yield updatedEclReturn

  def upsertAdditionalInfo(
    info: AdditionalInfo
  )(implicit hc: HeaderCarrier, request: AuthorisedRequest[_]): Future[AdditionalInfo] =
    additionalInfoConnector.upsertAdditionalInfo(info)

  def getAdditionalInfo(
    internalId: String
  )(implicit hc: HeaderCarrier, request: AuthorisedRequest[_]): Future[Option[AdditionalInfo]] =
    additionalInfoConnector
      .getAdditionalInfo(internalId)
      .map(info => Some(info))
      .recover(_ => None)

  def deleteAdditionalInfo(
    internalId: String
  )(implicit hc: HeaderCarrier, request: AuthorisedRequest[_]): Future[Unit] =
    additionalInfoConnector.deleteAdditionalInfo(internalId)
}
