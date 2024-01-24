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
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculateLiabilityRequest, CalculatedLiability}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EclCalculatorConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends BaseConnector
    with Retries {

  private val eclCalculatorUrl: String = s"${appConfig.eclCalculatorBaseUrl}/economic-crime-levy-calculator"

  def calculateLiability(amlRegulatedActivityLength: Int, relevantApLength: Int, relevantApRevenue: BigDecimal)(implicit
    hc: HeaderCarrier
  ): Future[CalculatedLiability] = {
    val body = CalculateLiabilityRequest(
      amlRegulatedActivityLength = amlRegulatedActivityLength,
      relevantApLength = relevantApLength,
      ukRevenue = relevantApRevenue.toLong
    )

    retryFor[CalculatedLiability]("ECL Calculator Connector - calculate liability")(retryCondition) {
      httpClient
        .post(url"$eclCalculatorUrl/calculate-liability")
        .withBody(Json.toJson(body))
        .executeAndDeserialise[CalculatedLiability]
    }
  }

}
