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
import com.typesafe.config.Config
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.KeyValue
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.{EclEnrolment, QueryKnownFactsRequest, QueryKnownFactsResponse}
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait EnrolmentStoreProxyConnector {
  def queryKnownFacts(eclRegistrationReference: String)(implicit hc: HeaderCarrier): Future[QueryKnownFactsResponse]
}

@Singleton
class EnrolmentStoreProxyConnectorImpl @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit
  ec: ExecutionContext
) extends EnrolmentStoreProxyConnector
    with BaseConnector
    with Retries {

  private val enrolmentStoreUrl: String =
    s"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store-proxy/enrolment-store"

  def queryKnownFacts(eclRegistrationReference: String)(implicit hc: HeaderCarrier): Future[QueryKnownFactsResponse] = {
    val body = QueryKnownFactsRequest(
      EclEnrolment.ServiceName,
      knownFacts = Seq(
        KeyValue(key = EclEnrolment.IdentifierKey, value = eclRegistrationReference)
      )
    )

    retryFor[QueryKnownFactsResponse]("Enrolment store - query known facts")(retryCondition) {
      httpClient
        .post(url"$enrolmentStoreUrl/enrolments")
        .withBody(Json.toJson(body))
        .executeAndDeserialise[QueryKnownFactsResponse]
    }
  }

}
