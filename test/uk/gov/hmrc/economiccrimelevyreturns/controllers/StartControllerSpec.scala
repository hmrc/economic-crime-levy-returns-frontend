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

package uk.gov.hmrc.economiccrimelevyreturns.controllers

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.{EclAccountConnector, EclReturnsConnector}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.ObligationDetails
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclReturnsService, EnrolmentStoreProxyService}
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AlreadySubmittedReturnView, ChooseReturnPeriodView, NoObligationForPeriodView, StartView}

import java.time.LocalDate
import scala.concurrent.Future

class StartControllerSpec extends SpecBase {

  val mockEnrolmentStoreProxyService: EnrolmentStoreProxyService = mock[EnrolmentStoreProxyService]
  val mockEclAccountConnector: EclAccountConnector               = mock[EclAccountConnector]
  val mockEclReturnsService: EclReturnsService                   = mock[EclReturnsService]
  val mockEclReturnsConnector: EclReturnsConnector               = mock[EclReturnsConnector]

  val view: StartView                                        = app.injector.instanceOf[StartView]
  val alreadySubmittedReturnView: AlreadySubmittedReturnView = app.injector.instanceOf[AlreadySubmittedReturnView]
  val noObligationForPeriodView: NoObligationForPeriodView   = app.injector.instanceOf[NoObligationForPeriodView]
  val chooseReturnPeriodView: ChooseReturnPeriodView         = app.injector.instanceOf[ChooseReturnPeriodView]

  val controller = new StartController(
    mcc,
    fakeAuthorisedAction(internalId),
    mockEnrolmentStoreProxyService,
    mockEclAccountConnector,
    mockEclReturnsService,
    mockEclReturnsConnector,
    alreadySubmittedReturnView,
    noObligationForPeriodView,
    chooseReturnPeriodView,
    view
  )

  "onPageLoad" should {
    "return OK and the start view when the period key is for an open obligation" in forAll {
      (periodKey: String, obligationDetails: ObligationDetails, eclRegistrationDate: LocalDate) =>
        when(
          mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
        )
          .thenReturn(Future.successful(eclRegistrationDate))

        val result: Future[Result] = controller.onPageLoad(periodKey)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(
          eclRegistrationReference,
          ViewUtils.formatLocalDate(eclRegistrationDate)(messages),
          ViewUtils.formatObligationPeriodYears(obligationDetails)
        )(fakeRequest, messages).toString
    }
  }

}
