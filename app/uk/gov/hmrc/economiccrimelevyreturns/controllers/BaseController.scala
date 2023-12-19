package uk.gov.hmrc.economiccrimelevyreturns.controllers

import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results.Status

import scala.concurrent.{ExecutionContext, Future}
import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError

trait BaseController {

  implicit class ResponseHandler[R](value: EitherT[Future, ResponseError, R]) {

    def convertToResultWithJsonBody(
                                     statusCode: Int
                                   )(implicit ec: ExecutionContext, writes: Writes[R]): Future[Result] =
      value.fold(
        err => Status(err.code.statusCode)(Json.toJson(err)),
        response => Status(statusCode)(Json.toJson(response))
      )
  }

}
