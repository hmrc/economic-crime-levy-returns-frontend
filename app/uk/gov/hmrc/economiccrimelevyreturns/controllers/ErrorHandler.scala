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

import scala.concurrent.{ExecutionContext, Future}
import cats.data.EitherT
import play.api.Logging
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{BadGateway, DataHandlingError, EclAccountError, InternalServiceError, LiabilityCalculationError, ResponseError}

trait ErrorHandler extends Logging {

  implicit class ErrorConvertor[E, R](value: EitherT[Future, E, R]) {

    def asResponseError(implicit c: Converter[E], ec: ExecutionContext): EitherT[Future, ResponseError, R] =
      value.leftMap(c.convert).leftSemiflatTap {
        case InternalServiceError(message, _, cause) =>
          val causeText = cause
            .map { ex =>
              s"""
                 |Message: ${ex.getMessage}
                 |Trace: ${ex.getStackTrace.mkString(System.lineSeparator())}
                 |""".stripMargin
            }
            .getOrElse("No exception is available")
          logger.error(s"""Internal Server Error: $message
                          |
                          |$causeText""".stripMargin)
          Future.successful(())
        case BadGateway(message, _, responseCode)    =>
          val causeText = s"""
                             |Message: $message
                             |Upstream status code: $responseCode
                             |""".stripMargin

          logger.error(s"""Bad gateway: $message
                          |
                          |$causeText""".stripMargin)
          Future.successful(())
        case _                                       => Future.successful(())
      }
  }

  trait Converter[E] {
    def convert(error: E): ResponseError
  }

  implicit val dataHandlingErrorConverter: Converter[DataHandlingError] =
    new Converter[DataHandlingError] {
      override def convert(error: DataHandlingError): ResponseError = error match {
        case DataHandlingError.BadGateway(cause, code)           => ResponseError.badGateway(cause, code)
        case DataHandlingError.InternalUnexpectedError(cause, _) => ResponseError.internalServiceError(cause = cause)
        case DataHandlingError.NotFound(message)                 => ResponseError.internalServiceError(message)
      }
    }

  implicit val eclAccountErrorConverter: Converter[EclAccountError] =
    new Converter[EclAccountError] {
      override def convert(error: EclAccountError): ResponseError = error match {
        case EclAccountError.InternalUnexpectedError(cause, _) => ResponseError.internalServiceError(cause = cause)
        case EclAccountError.BadGateway(cause, code)           => ResponseError.badGateway(cause, code)
      }
    }

  implicit val liabilityCalculationErrorConverter: Converter[LiabilityCalculationError] =
    new Converter[LiabilityCalculationError] {
      override def convert(error: LiabilityCalculationError): ResponseError = error match {
        case LiabilityCalculationError.BadRequest(message)               => ResponseError.badRequestError(message)
        case LiabilityCalculationError.InternalUnexpectedError(cause, _) =>
          ResponseError.internalServiceError(cause = cause)
        case LiabilityCalculationError.BadGateway(cause, code)           => ResponseError.badGateway(cause, code)

      }
    }
}
