/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EclReturnsService @Inject() (eclReturnsConnector: EclReturnsConnector)(implicit
  ec: ExecutionContext
) {

  def getOrCreateReturn(internalId: String)(implicit hc: HeaderCarrier): Future[EclReturn] =
    eclReturnsConnector.getReturn(internalId).flatMap {
      case Some(eclReturn) => Future.successful(eclReturn)
      case None            => eclReturnsConnector.upsertReturn(EclReturn(internalId))
    }
}
