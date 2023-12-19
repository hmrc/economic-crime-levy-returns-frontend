package uk.gov.hmrc.economiccrimelevyreturns.models.errors

import play.api.http.Status._
import play.api.libs.json._

sealed abstract class ErrorCode(val code: String, val statusCode: Int) extends Product with Serializable

object ErrorCode {
  case object BadRequest extends ErrorCode("BAD_REQUEST", BAD_REQUEST)
  case object BadGateway extends ErrorCode("BAD_REQUEST", BAD_GATEWAY)

  case object NotFound extends ErrorCode("NOT_FOUND", NOT_FOUND)

  case object Forbidden extends ErrorCode("FORBIDDEN", FORBIDDEN)

  case object InternalServerError extends ErrorCode("INTERNAL_SERVER_ERROR", INTERNAL_SERVER_ERROR)

  case object GatewayTimeout extends ErrorCode("GATEWAY_TIMEOUT", GATEWAY_TIMEOUT)

  case object UnsupportedMediaType extends ErrorCode("UNSUPPORTED_MEDIA_TYPE", UNSUPPORTED_MEDIA_TYPE)

  case object Unauthorized extends ErrorCode("UNAUTHORIZED", UNAUTHORIZED)

  lazy val errorCodes: Seq[ErrorCode] = Seq(
    BadRequest,
    NotFound,
    Forbidden,
    InternalServerError,
    GatewayTimeout,
    UnsupportedMediaType,
    Unauthorized
  )

  implicit val errorCodeWrites: Writes[ErrorCode] = Writes { errorCode =>
    JsString(errorCode.code)
  }

  implicit val errorCodeReads: Reads[ErrorCode] = Reads { errorCode =>
    errorCodes
      .find(value => value.code == errorCode.asInstanceOf[JsString].value)
      .map(errorCode => JsSuccess(errorCode))
      .getOrElse(JsError())
  }

}
