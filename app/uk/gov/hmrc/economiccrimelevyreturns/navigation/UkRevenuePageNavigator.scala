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

package uk.gov.hmrc.economiccrimelevyreturns.navigation

import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.Band.Small
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, Mode, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.services.EclLiabilityService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkRevenuePageNavigator @Inject() (
  eclLiabilityService: EclLiabilityService,
  eclReturnsConnector: EclReturnsConnector
)(implicit
  ec: ExecutionContext
) extends AsyncPageNavigator
    with FrontendHeaderCarrierProvider {

  override protected def navigateInNormalMode(eclReturn: EclReturn)(implicit request: RequestHeader): Future[Call] =
    navigate(eclReturn, NormalMode)

  override protected def navigateInCheckMode(eclReturn: EclReturn)(implicit request: RequestHeader): Future[Call] =
    navigate(eclReturn, CheckMode)

  private def navigate(eclReturn: EclReturn, mode: Mode)(implicit request: RequestHeader): Future[Call] =
    eclReturn.relevantApRevenue match {
      case Some(_) =>
        eclLiabilityService.calculateLiability(eclReturn) match {
          case Some(f) =>
            f.flatMap { updatedReturn =>
              updatedReturn.calculatedLiability match {
                case Some(calculatedLiability) if calculatedLiability.calculatedBand == Small =>
                  clearAmlActivityAnswers(updatedReturn, mode)
                case Some(_)                                                                  => navigateLiable(updatedReturn, mode)
                case _                                                                        => Future.successful(routes.NotableErrorController.answersAreInvalid())
              }
            }
          case None    => Future.successful(routes.NotableErrorController.answersAreInvalid())
        }
      case _       => Future.successful(routes.NotableErrorController.answersAreInvalid())
    }

  private def clearAmlActivityAnswers(eclReturn: EclReturn, mode: Mode)(implicit request: RequestHeader): Future[Call] =
    eclReturnsConnector
      .upsertReturn(eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = None, amlRegulatedActivityLength = None))
      .map(_ => routes.AmountDueController.onPageLoad(mode))

  private def navigateLiable(eclReturn: EclReturn, mode: Mode): Future[Call] =
    mode match {
      case NormalMode => Future.successful(routes.AmlRegulatedActivityController.onPageLoad(mode))
      case CheckMode  =>
        eclReturn.carriedOutAmlRegulatedActivityForFullFy match {
          case Some(_) => Future.successful(routes.AmountDueController.onPageLoad(mode))
          case None    => Future.successful(routes.AmlRegulatedActivityController.onPageLoad(mode))
        }
    }

}
