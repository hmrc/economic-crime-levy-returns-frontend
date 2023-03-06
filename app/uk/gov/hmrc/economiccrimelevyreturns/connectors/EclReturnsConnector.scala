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
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculateLiabilityRequest, CalculatedLiability, EclReturn}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EclReturnsConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit ec: ExecutionContext)
    extends Logging {

  private val eclReturnsUrl: String = s"${appConfig.eclReturnsBaseUrl}/economic-crime-levy-returns"

  def getReturn(internalId: String)(implicit hc: HeaderCarrier): Future[Option[EclReturn]] =
    httpClient.GET[Option[EclReturn]](
      s"$eclReturnsUrl/returns/$internalId"
    )

  def upsertReturn(eclReturn: EclReturn)(implicit hc: HeaderCarrier): Future[EclReturn] =
    httpClient.PUT[EclReturn, EclReturn](
      s"$eclReturnsUrl/returns",
      eclReturn
    )

  def deleteReturn(internalId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .DELETE[HttpResponse](
        s"$eclReturnsUrl/returns/$internalId"
      )
      .map(_ => ())

  def calculateLiability(amlRegulatedActivityLength: Int, relevantApLength: Int, relevantApRevenue: Long)(implicit
    hc: HeaderCarrier
  ): Future[CalculatedLiability] =
    httpClient.POST[CalculateLiabilityRequest, CalculatedLiability](
      s"$eclReturnsUrl/calculate-liability",
      CalculateLiabilityRequest(
        amlRegulatedActivityLength = amlRegulatedActivityLength,
        relevantApLength = relevantApLength,
        ukRevenue = relevantApRevenue
      )
    )

  def getReturnValidationErrors(
    internalId: String
  )(implicit hc: HeaderCarrier): Future[Option[DataValidationErrors]] =
    httpClient
      .GET[Either[UpstreamErrorResponse, HttpResponse]](
        s"$eclReturnsUrl/returns/$internalId/validation-errors"
      )
      .map {
        case Right(httpResponse) =>
          httpResponse.status match {
            case NO_CONTENT => None
            case OK         =>
              logger.warn(s"Data validation errors:\n${Json.prettyPrint(httpResponse.json)}")
              Some(httpResponse.json.as[DataValidationErrors])
            case s          => throw new HttpException(s"Unexpected response with HTTP status $s", s)
          }
        case Left(e)             => throw e
      }

}
