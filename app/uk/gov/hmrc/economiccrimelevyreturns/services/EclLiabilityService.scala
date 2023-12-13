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

package uk.gov.hmrc.economiccrimelevyreturns.services

import cats.data.EitherT
import play.api.mvc.RequestHeader
import uk.gov.hmrc.economiccrimelevyreturns.connectors.{EclCalculatorConnector, ReturnsConnector}
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.LiabilityCalculationError
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class EclLiabilityService @Inject() (
  eclReturnsConnector: ReturnsConnector,
  eclCalculatorConnector: EclCalculatorConnector
)(implicit
  ec: ExecutionContext
) extends FrontendHeaderCarrierProvider {

  private val FullYear: Option[Int] = Some(365)

  def calculateLiability(eclReturn: EclReturn)(implicit request: RequestHeader): Option[Future[EclReturn]] =
    for {
      relevantAp12Months                     <- eclReturn.relevantAp12Months
      relevantApLength                       <- if (relevantAp12Months) FullYear else eclReturn.relevantApLength
      relevantApRevenue                      <- eclReturn.relevantApRevenue
      carriedOutAmlRegulatedActivityForFullFy = eclReturn.carriedOutAmlRegulatedActivityForFullFy.getOrElse(true)
      amlRegulatedActivityLength             <- calculateAmlRegulatedActivityLength(
                                                  carriedOutAmlRegulatedActivityForFullFy,
                                                  eclReturn.amlRegulatedActivityLength
                                                )
    } yield calculateLiabilityAndUpsertReturn(
      relevantApLength = relevantApLength,
      relevantApRevenue = relevantApRevenue,
      amlRegulatedActivityLength = amlRegulatedActivityLength,
      eclReturn = eclReturn
    )

  private def calculateLiabilityAndUpsertReturn(
    relevantApLength: Int,
    relevantApRevenue: BigDecimal,
    amlRegulatedActivityLength: Int,
    eclReturn: EclReturn
  )(implicit request: RequestHeader): Future[EclReturn] =
    eclCalculatorConnector.calculateLiability(amlRegulatedActivityLength, relevantApLength, relevantApRevenue).flatMap {
      calculatedLiability =>
        eclReturnsConnector.upsertReturn(eclReturn.copy(calculatedLiability = Some(calculatedLiability)))
    }

  def getCalculatedLiability(relevantApLength: Int, relevantApRevenue: BigDecimal, amlRegulatedActivityLength: Int)(
    implicit hc: HeaderCarrier
  ): EitherT[Future, LiabilityCalculationError, CalculatedLiability] =
    EitherT {
      eclCalculatorConnector
        .calculateLiability(amlRegulatedActivityLength, relevantApLength, relevantApRevenue)
        .map {
          Right(_)
        }
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(LiabilityCalculationError.BadGateway(reason = message, code = code))
          case NonFatal(thr) => Left(LiabilityCalculationError.InternalUnexpectedError(Some(thr)))
        }
    }

  def calculateAmlRegulatedActivityLength(
    carriedOutAmlRegulatedActivityForFullFy: Boolean,
    optAmlRegulatedActivityLength: Option[Int]
  ): Option[Int] =
    (carriedOutAmlRegulatedActivityForFullFy, optAmlRegulatedActivityLength) match {
      case (false, None)    => Some(0)
      case (true, _)        => FullYear
      case (false, Some(_)) => optAmlRegulatedActivityLength
    }

}
