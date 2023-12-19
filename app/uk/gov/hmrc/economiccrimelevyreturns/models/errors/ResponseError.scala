package uk.gov.hmrc.economiccrimelevyreturns.models.errors

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{OWrites, Reads, __}
import uk.gov.hmrc.http.UpstreamErrorResponse

sealed abstract class ResponseError extends Product with Serializable {
  def message: String
  def code: ErrorCode
}

object ResponseError {

  val MessageFieldName = "message"
  val CodeFieldName    = "code"

  def badRequestError(message: String): ResponseError =
    StandardError(message, ErrorCode.BadRequest)

  def notFoundError(message: String): ResponseError =
    StandardError(message, ErrorCode.NotFound)

  def unauthorized(message: String): ResponseError =
    StandardError(message, ErrorCode.Unauthorized)

  def badGateway(message: String, code: Int): ResponseError =
    BadGateway(message, ErrorCode.BadGateway, code)

  def upstreamServiceError(
                            message: String = "Internal server error",
                            code: ErrorCode = ErrorCode.InternalServerError,
                            cause: UpstreamErrorResponse
                          ): ResponseError =
    UpstreamServiceError(message, code, cause)

  def internalServiceError(
                            message: String = "Internal server error",
                            code: ErrorCode = ErrorCode.InternalServerError,
                            cause: Option[Throwable] = None
                          ): ResponseError =
    InternalServiceError(message, code, cause)

  implicit val errorWrites: OWrites[ResponseError] =
    (
      (__ \ MessageFieldName).write[String] and
        (__ \ CodeFieldName).write[ErrorCode]
      )(unlift(ResponseError.unapply))

  implicit val standardErrorReads: Reads[StandardError] =
    (
      (__ \ MessageFieldName).read[String] and
        (__ \ CodeFieldName).read[ErrorCode]
      )(StandardError.apply _)

  def unapply(error: ResponseError): Option[(String, ErrorCode)] = Some((error.message, error.code))
}

case class StandardError(message: String, code: ErrorCode) extends ResponseError

case class UpstreamServiceError(
                                 message: String = "Internal server error",
                                 code: ErrorCode = ErrorCode.InternalServerError,
                                 cause: UpstreamErrorResponse
                               ) extends ResponseError

case class BadGateway(
                       message: String = "Internal server error",
                       code: ErrorCode = ErrorCode.BadGateway,
                       responseCode: Int
                     ) extends ResponseError

object BadGateway {
  def causedBy(message: String, code: Int): ResponseError =
    ResponseError.badGateway(message = message, code = code)
}

object UpstreamServiceError {
  def causedBy(cause: UpstreamErrorResponse): ResponseError =
    ResponseError.upstreamServiceError(cause = cause)
}

case class InternalServiceError(
                                 message: String = "Internal server error",
                                 code: ErrorCode = ErrorCode.InternalServerError,
                                 cause: Option[Throwable] = None
                               ) extends ResponseError

object InternalServiceError {
  def causedBy(cause: Throwable): ResponseError =
    ResponseError.internalServiceError(cause = Some(cause))
}
