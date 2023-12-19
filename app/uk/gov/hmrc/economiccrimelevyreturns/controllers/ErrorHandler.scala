package uk.gov.hmrc.economiccrimelevyreturns.controllers

import scala.concurrent.{ExecutionContext, Future}
import cats.data.EitherT
import play.api.Logging
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{BadGateway, DataHandlingError, InternalServiceError, LiabilityCalculationError, ResponseError}

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
        case DataHandlingError.InternalUnexpectedError(cause) => ResponseError.internalServiceError(cause = cause)
      }
    }

  implicit val liabilityCalculationErrorConverter: Converter[LiabilityCalculationError] =
    new Converter[LiabilityCalculationError] {
      override def convert(error: LiabilityCalculationError): ResponseError = error match {
        case LiabilityCalculationError.BadRequest(message) => ResponseError.badRequestError(message)
      }
    }

//  implicit val enrolmentStoreErrorConverter: Converter[EnrolmentStoreError] =
//    new Converter[EnrolmentStoreError] {
//      override def convert(error: EnrolmentStoreError): ResponseError = error match {
//        case EnrolmentStoreError.BadGateway(cause, statusCode)           => ResponseError.badGateway(cause, statusCode)
//        case EnrolmentStoreError.InternalUnexpectedError(message, cause) =>
//          ResponseError.internalServiceError(message = message, cause = cause)
//      }
//    }
}
