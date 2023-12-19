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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, SessionData}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, Retries, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionDataConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit
  ec: ExecutionContext
) extends BaseConnector
    with Retries {

  private val eclReturnsUrl: String =
    s"${appConfig.eclReturnsBaseUrl}/economic-crime-levy-returns"

  def get(internalId: String)(implicit hc: HeaderCarrier): Future[SessionData] =
    retryFor[SessionData]("Session Data Connector - get")(retryCondition) {
      httpClient.get(url"$eclReturnsUrl/session/$internalId").executeAndDeserialise[SessionData]
    }

  def upsert(session: SessionData)(implicit hc: HeaderCarrier): Future[Unit] =
    retryFor[Unit]("Session Data Connector - upsert")(retryCondition) {
      httpClient.put(url"$eclReturnsUrl/session").withBody(Json.toJson(session)).executeAndExpect(OK)
    }

  def delete(internalId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    retryFor[Unit]("Session Data Connector - delete")(retryCondition) {
      httpClient.delete(url"$eclReturnsUrl/session/$internalId").executeAndExpect(OK)
    }
}
