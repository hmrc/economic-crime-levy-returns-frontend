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

import cats.data.OptionT
import play.api.http.Status.{ACCEPTED, CREATED, OK}
import play.api.libs.json.{JsResult, Reads}
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

trait BaseConnector {

  def retryCondition: PartialFunction[Exception, Boolean] = {
    case e: UpstreamErrorResponse if UpstreamErrorResponse.Upstream5xxResponse.unapply(e).isDefined => true
  }

  implicit class HttpResponseHelpers(response: HttpResponse) {

    def error[A]: Future[A] =
      Future.failed(UpstreamErrorResponse(response.body, response.status))

    def as[A](implicit reads: Reads[A]): Future[A] =
      response.json
        .validate[A]
        .map(result => Future.successful(result))
        .recoverTotal(error => Future.failed(JsResult.Exception(error)))

    def asOption[A](implicit reads: Reads[A]): Future[Option[A]] =
      response.json
        .validateOpt[A]
        .map(result => Future.successful(result))
        .recoverTotal(error => Future.failed(JsResult.Exception(error)))
  }

  implicit class RequestBuilderHelpers(requestBuilder: RequestBuilder) {
    def executeAndDeserialise[T](implicit ec: ExecutionContext, reads: Reads[T]): Future[T] =
      requestBuilder
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case OK | CREATED | ACCEPTED => response.as[T]
            case _                       => response.error
          }
        }

    def executeAndDeserialiseOpt[T](implicit ec: ExecutionContext, reads: Reads[T]): OptionT[Future, T] =
      OptionT {
        requestBuilder
          .execute[HttpResponse]
          .flatMap { response =>
            response.status match {
              case OK | CREATED | ACCEPTED => response.asOption[T]
              case _                       =>
                response.error
            }
          }
      }

    def executeAndExpect(expected: Int)(implicit ec: ExecutionContext): Future[Unit] =
      requestBuilder
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case `expected` => Future.successful(())
            case _          => response.error
          }
        }
  }
}
