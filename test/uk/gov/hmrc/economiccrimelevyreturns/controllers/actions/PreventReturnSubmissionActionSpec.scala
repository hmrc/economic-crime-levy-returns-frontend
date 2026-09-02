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

import org.mockito.Mockito.when
import play.api.Configuration
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, ObligationDetails, Open}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest
import play.api.test.Helpers.SEE_OTHER

import java.time.LocalDate
import scala.concurrent.Future

class PreventReturnSubmissionActionSpec extends SpecBase {

  val mockAppConfig: AppConfig = mock[AppConfig]

  val accountUrl = "http://localhost:14008/economic-crime-levy-account"

  when(mockAppConfig.eclAccountUrl).thenReturn(accountUrl)

  class TestPreventReturnSubmissionAction(
    configuration: Configuration,
    appConfig: AppConfig
  ) extends PreventReturnSubmissionAction(configuration, appConfig) {

    override def filter[A](request: ReturnDataRequest[A]): Future[Option[Result]] =
      super.filter(request)
  }

  private def action(
    preventionEnabled: Boolean,
    preventedTaxYears: Seq[String]
  ): TestPreventReturnSubmissionAction = {

    val configuration = Configuration.from(
      Map(
        "features.preventReturnSubmissionEnabled" -> preventionEnabled,
        "features.preventedReturnTaxYears"        -> preventedTaxYears
      )
    )

    new TestPreventReturnSubmissionAction(
      configuration,
      mockAppConfig
    )
  }

  private def obligationDetails(
    from: LocalDate,
    to: LocalDate
  ): ObligationDetails =
    ObligationDetails(
      status = Open,
      inboundCorrespondenceFromDate = from,
      inboundCorrespondenceToDate = to,
      inboundCorrespondenceDateReceived = None,
      inboundCorrespondenceDueDate = to.plusMonths(6),
      periodKey = "test-period-key"
    )

  private def requestWithObligation(
    obligationDetails: Option[ObligationDetails]
  ): ReturnDataRequest[_] = {

    val eclReturn =
      EclReturn
        .empty(internalId, None)
        .copy(obligationDetails = obligationDetails)

    ReturnDataRequest(
      fakeRequest,
      internalId,
      eclReturn,
      None,
      eclRegistrationReference,
      Some(periodKey)
    )
  }

  "filter" should {

    "allow a prevented tax year when return prevention is disabled" in {

      val request = requestWithObligation(
        Some(
          obligationDetails(
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2027, 3, 31)
          )
        )
      )

      val result =
        await(
          action(
            preventionEnabled = false,
            preventedTaxYears = Seq("2026-2027")
          ).filter(request)
        )

      result shouldBe None
    }

    "allow a tax year that is not configured for prevention" in {

      val request = requestWithObligation(
        Some(
          obligationDetails(
            LocalDate.of(2025, 4, 1),
            LocalDate.of(2026, 3, 31)
          )
        )
      )

      val result =
        await(
          action(
            preventionEnabled = true,
            preventedTaxYears = Seq("2026-2027")
          ).filter(request)
        )

      result shouldBe None
    }

    "redirect a tax year configured for prevention to the ECL account" in {

      val request = requestWithObligation(
        Some(
          obligationDetails(
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2027, 3, 31)
          )
        )
      )

      val result =
        await(
          action(
            preventionEnabled = true,
            preventedTaxYears = Seq("2026-2027")
          ).filter(request)
        )

      result.value.header.status                  shouldBe SEE_OTHER
      result.value.header.headers.get("Location") shouldBe Some(accountUrl)
    }

    "allow the request when there are no obligation details" in {

      val request = requestWithObligation(None)

      val result =
        await(
          action(
            preventionEnabled = true,
            preventedTaxYears = Seq("2026-2027")
          ).filter(request)
        )

      result shouldBe None
    }
  }
}
