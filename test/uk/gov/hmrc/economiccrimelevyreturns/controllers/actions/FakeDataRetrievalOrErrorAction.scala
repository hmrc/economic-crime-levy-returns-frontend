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

package uk.gov.hmrc.economiccrimelevyreturns.controllers.actions

import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, EclReturn}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.{AuthorisedRequest, ReturnDataRequest}

import scala.concurrent.{ExecutionContext, Future}

class FakeDataRetrievalOrErrorAction(data: EclReturn, periodKey: Option[String] = None, dataRetrievalFailure: Boolean)
    extends DataRetrievalOrErrorAction {

  override implicit val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  override protected def refine[A](request: AuthorisedRequest[A]): Future[Either[Result, ReturnDataRequest[A]]] =
    if (dataRetrievalFailure) {
      Future(Left(Redirect(getRedirectUrl(request))))
    } else {
      Future(
        Right(
          ReturnDataRequest(
            request.request,
            request.internalId,
            data,
            None,
            request.eclRegistrationReference,
            periodKey
          )
        )
      )
    }

  private def getRedirectUrl(request: AuthorisedRequest[_]) =
    request.uri match {
      case amendUrl if amendUrl == routes.CheckYourAnswersController.onPageLoad(AmendReturn).url =>
        routes.NotableErrorController.returnAmendmentAlreadyRequested()
      case _                                                                                     => routes.NotableErrorController.eclReturnAlreadySubmitted()
    }

}
