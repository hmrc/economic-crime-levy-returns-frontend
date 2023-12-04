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

package uk.gov.hmrc.economiccrimelevyreturns.connectors

import play.api.Logging
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionData
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionDataConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit
  ec: ExecutionContext
) extends Logging {

  private val eclReturnsUrl: String =
    s"${appConfig.eclReturnsBaseUrl}/economic-crime-levy-returns"

  def get(internalId: String)(implicit hc: HeaderCarrier): Future[SessionData] =
    httpClient.GET[SessionData](
      s"$eclReturnsUrl/session/$internalId"
    )

  def upsert(session: SessionData)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient.PUT[SessionData, Unit](
      s"$eclReturnsUrl/session",
      session
    )

  def delete(internalId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .DELETE[Unit](
        s"$eclReturnsUrl/session/$internalId"
      )
}
