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
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.utils.EclTaxYear
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmlRegulatedActivityPageNavigator @Inject() (eclReturnsConnector: EclReturnsConnector)(implicit
  ec: ExecutionContext
) extends AsyncPageNavigator
    with FrontendHeaderCarrierProvider {

  override protected def navigateInNormalMode(
    eclReturn: EclReturn
  )(implicit request: RequestHeader): Future[Call] =
    eclReturn.carriedOutAmlRegulatedActivityForFullFy match {
      case Some(true)  => calculateLiability(eclReturn).map(_ => ???)
      case Some(false) => ???
      case _           => Future.successful(routes.NotableErrorController.answersAreInvalid())
    }

  override protected def navigateInCheckMode(
    eclReturn: EclReturn
  )(implicit request: RequestHeader): Future[Call] =
    eclReturn.carriedOutAmlRegulatedActivityForFullFy match {
      case Some(true)  => Future.successful(routes.CheckYourAnswersController.onPageLoad())
      case Some(false) => ???
      case _           => Future.successful(routes.NotableErrorController.answersAreInvalid())
    }

  private def calculateLiability(eclReturn: EclReturn)(implicit request: RequestHeader): Future[Unit] =
    (eclReturn.relevantAp12Months, eclReturn.relevantApLength, eclReturn.relevantApRevenue) match {
      case (Some(true), _, Some(relevantApRevenue))                       =>
        eclReturnsConnector.calculateLiability(EclTaxYear.YearInDays, EclTaxYear.YearInDays, relevantApRevenue).map {
          calculatedLiability =>
            eclReturnsConnector
              .upsertReturn(eclReturn.copy(calculatedLiability = Some(calculatedLiability)))
              .map(_ => ())
        }
      case (Some(false), Some(relevantApLength), Some(relevantApRevenue)) =>
        eclReturnsConnector.calculateLiability(EclTaxYear.YearInDays, relevantApLength, relevantApRevenue).map {
          calculatedLiability =>
            eclReturnsConnector
              .upsertReturn(eclReturn.copy(calculatedLiability = Some(calculatedLiability)))
              .map(_ => ())
        }
      case _                                                              => throw new IllegalStateException("Relevant AP answers not found in ECL return")
    }

}
