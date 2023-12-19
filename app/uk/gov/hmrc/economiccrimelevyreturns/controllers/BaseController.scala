package uk.gov.hmrc.economiccrimelevyreturns.controllers

import cats.data.EitherT
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, RequestHeader, Result}
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, Mode}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError
import uk.gov.hmrc.economiccrimelevyreturns.navigation.{AsyncPageNavigator, PageNavigator}

import scala.concurrent.{ExecutionContext, Future}

trait BaseController {

  implicit class ResponseHandler(value: EitherT[Future, ResponseError, EclReturn]) {

    def convertToResult(mode: Mode, pageNavigator: PageNavigator
                            )(implicit ec: ExecutionContext): Future[Result] = {
      value.fold(
          _ => Future.successful(routes.NotableErrorController.answersAreInvalid()),
          eclReturn => pageNavigator.nextPage(mode, eclReturn)
        ).map(Redirect)
    }

    def convertToAsyncResult(mode: Mode, pageNavigator: AsyncPageNavigator
                                   )(implicit ec: ExecutionContext, request: RequestHeader): Future[Result] = {
      value.fold(
        _ => Future.successful(routes.NotableErrorController.answersAreInvalid()),
        eclReturn => pageNavigator.nextPage(mode, eclReturn)
      ).flatMap(r => r)
        .map(Redirect)
    }
  }
}
