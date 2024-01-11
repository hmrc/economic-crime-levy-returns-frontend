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
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculateLiabilityRequest, CalculatedLiability, EclReturn, SubmitEclReturnResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._
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
    retryFor[EclReturn]("ECL Returns Connector - get return")(retryCondition) {
      httpClient.get(url"$eclReturnsUrl/returns/$internalId").executeAndDeserialise[EclReturn]
    }

  def upsertReturn(eclReturn: EclReturn)(implicit hc: HeaderCarrier): Future[EclReturn] =
    retryFor[EclReturn]("ECL Returns Connector - upsert return")(retryCondition) {
      httpClient.put(url"$eclReturnsUrl/returns").withBody(Json.toJson(eclReturn)).executeAndDeserialise[EclReturn]
    }

  def deleteReturn(internalId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    retryFor[Unit]("ECL Returns Connector - delete return")(retryCondition) {
      httpClient.delete(url"$eclReturnsUrl/returns/$internalId").executeAndExpect(OK)
    }

  def calculateLiability(amlRegulatedActivityLength: Int, relevantApLength: Int, relevantApRevenue: Long)(implicit
    hc: HeaderCarrier
  ): Future[CalculatedLiability] = {
    val calculatedLiabilityRequest = CalculateLiabilityRequest(
      amlRegulatedActivityLength = amlRegulatedActivityLength,
      relevantApLength = relevantApLength,
      ukRevenue = relevantApRevenue
    )

    retryFor[CalculatedLiability]("ECL Returns Connector - calculate liability")(retryCondition) {
      httpClient
        .post(url"$eclReturnsUrl/calculate-liability")
        .withBody(Json.toJson(calculatedLiabilityRequest))
        .executeAndDeserialise[CalculatedLiability]
    }
  }

  def getReturnValidationErrors(
    internalId: String
  )(implicit hc: HeaderCarrier): Future[Option[DataValidationError]] =
    retryFor[Option[DataValidationError]]("ECL Returns Connector - get return validation errors")(retryCondition) {
      httpClient
        .get(url"$eclReturnsUrl/returns/$internalId/validation-errors")
        .executeAndDeserialiseOpt[DataValidationError]
    }

  def submitReturn(internalId: String)(implicit hc: HeaderCarrier): Future[SubmitEclReturnResponse] =
    retryFor[SubmitEclReturnResponse]("ECL Returns Connector - submit return")(retryCondition) {
      httpClient.post(url"$eclReturnsUrl/submit-return/$internalId").executeAndDeserialise[SubmitEclReturnResponse]
    }

}
