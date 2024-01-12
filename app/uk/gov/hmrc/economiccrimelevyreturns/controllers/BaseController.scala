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

package uk.gov.hmrc.economiccrimelevyreturns.controllers

import cats.data.EitherT
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Redirect, Status}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, Mode}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{InternalServiceError, ResponseError, StandardError}
import uk.gov.hmrc.economiccrimelevyreturns.navigation.PageNavigator

import scala.concurrent.{ExecutionContext, Future}

trait BaseController {

  implicit class ResponseHandler(value: EitherT[Future, ResponseError, EclReturn]) {

    def convertToResult(mode: Mode, pageNavigator: PageNavigator)(implicit
      ec: ExecutionContext,
      request: Request[_]
    ): Future[Result] =
      value
        .fold(
          error => Status(error.code.statusCode)(Json.toJson(error)),
          result => Redirect(pageNavigator.nextPage(mode, result))
        )
  }
}
