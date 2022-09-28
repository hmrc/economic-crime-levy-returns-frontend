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

package uk.gov.hmrc.economiccrimelevyreturns.connectors

import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EclReturnsConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit ec: ExecutionContext) {

  private val eclReturnsUrl: String = s"${appConfig.eclReturnsBaseUrl}/economic-crime-levy-returns/returns"

  def getReturn(internalId: String)(implicit hc: HeaderCarrier): Future[Option[EclReturn]] =
    httpClient.GET[Option[EclReturn]](
      s"$eclReturnsUrl/$internalId"
    )

  def upsertReturn(eclReturn: EclReturn)(implicit hc: HeaderCarrier): Future[EclReturn] =
    httpClient.PUT[EclReturn, EclReturn](
      eclReturnsUrl,
      eclReturn
    )

  def deleteReturn(internalId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .DELETE[HttpResponse](
        s"$eclReturnsUrl/$internalId"
      )
      .map(_ => ())

}
