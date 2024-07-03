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

import org.apache.pekko.actor.ActorSystem
import cats.data.OptionT
import com.typesafe.config.Config
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnsConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends BaseConnector
    with Retries {

  private val eclReturnsUrl: String = s"${appConfig.eclReturnsBaseUrl}/economic-crime-levy-returns"

  def getReturn(internalId: String)(implicit hc: HeaderCarrier): Future[EclReturn] =
    httpClient.get(url"$eclReturnsUrl/returns/$internalId").executeAndDeserialise[EclReturn]

  def upsertReturn(eclReturn: EclReturn)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient.put(url"$eclReturnsUrl/returns").withBody(Json.toJson(eclReturn)).executeAndExpect(NO_CONTENT)

  def deleteReturn(internalId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient.delete(url"$eclReturnsUrl/returns/$internalId").executeAndExpect(NO_CONTENT)

  def validateEclReturn(
    internalId: String
  )(implicit hc: HeaderCarrier): OptionT[Future, String] =
    httpClient
      .get(url"$eclReturnsUrl/returns/$internalId/validation-errors")
      .executeAndDeserialiseOpt[String]

  def submitReturn(internalId: String)(implicit hc: HeaderCarrier): Future[SubmitEclReturnResponse] =
    httpClient.post(url"$eclReturnsUrl/submit-return/$internalId").executeAndDeserialise[SubmitEclReturnResponse]

  def getEclReturnSubmission(periodKey: String, eclReference: String)(implicit
    hc: HeaderCarrier
  ): Future[GetEclReturnSubmissionResponse] =
    httpClient
      .get(url"$eclReturnsUrl/submission/$periodKey/$eclReference")
      .executeAndDeserialise[GetEclReturnSubmissionResponse]
}
