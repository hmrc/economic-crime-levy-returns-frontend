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
import uk.gov.hmrc.economiccrimelevyreturns.models.AdditionalInfo
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdditionalInfoConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit ec: ExecutionContext)
    extends Logging
    with BaseConnector {

  private val additionalInfoUrl: String = s"${appConfig.eclReturnsBaseUrl}/economic-crime-levy-returns"

  def getAdditionalInfo(internalId: String)(implicit hc: HeaderCarrier): Future[AdditionalInfo] =
    executeAndDeserialise[AdditionalInfo](
      httpClient.GET[HttpResponse](
        s"$additionalInfoUrl/info/$internalId"
      )
    )

  def upsertAdditionalInfo(info: AdditionalInfo)(implicit hc: HeaderCarrier): Future[AdditionalInfo] =
    httpClient.PUT[AdditionalInfo, AdditionalInfo](
      s"$additionalInfoUrl/info",
      info
    )

  def deleteAdditionalInfo(internalId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .DELETE[HttpResponse](
        s"$additionalInfoUrl/info/$internalId"
      )
      .map(_ => ())

}
