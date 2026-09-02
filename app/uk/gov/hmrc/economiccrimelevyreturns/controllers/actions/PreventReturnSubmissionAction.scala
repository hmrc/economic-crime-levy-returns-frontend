/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.Configuration
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result, Results}
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreventReturnSubmissionAction @Inject() (
  configuration: Configuration,
  appConfig: AppConfig
)(implicit val executionContext: ExecutionContext)
    extends ActionFilter[ReturnDataRequest] {

  private val preventionEnabled: Boolean =
    configuration.get[Boolean]("features.preventReturnSubmissionEnabled")

  private val preventedTaxYears: Seq[String] =
    configuration.get[Seq[String]]("features.preventedReturnTaxYears")

  override protected def filter[A](request: ReturnDataRequest[A]): Future[Option[Result]] = {
    val isPrevented =
      request.eclReturn.obligationDetails.exists { details =>
        val taxYear =
          s"${details.inboundCorrespondenceFromDate.getYear}-${details.inboundCorrespondenceToDate.getYear}"
        preventionEnabled && preventedTaxYears.contains(taxYear)
      }

    if (isPrevented) {
      Future.successful(Some(Redirect(appConfig.eclAccountUrl)))
    } else {
      Future.successful(None)
    }
  }
}
