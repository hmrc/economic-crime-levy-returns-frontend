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
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EclAccountConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit ec: ExecutionContext)
    extends Logging {

  private val eclAccountUrl: String = s"${appConfig.eclAccountBaseUrl}/economic-crime-levy-account"

  def getObligations()(implicit hc: HeaderCarrier): Future[Option[ObligationData]] =
    httpClient.GET[Option[ObligationData]](
      s"$eclAccountUrl/obligation-data"
    )

}
