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
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.{EclAccountConnector, EclReturnsConnector}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.services.EclReturnsService
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{NoObligationForPeriodView, StartAmendView}

import scala.concurrent.Future

class StartAmendControllerSpec extends SpecBase {

  val mockEclAccountConnector: EclAccountConnector = mock[EclAccountConnector]
  val mockEclReturnsService: EclReturnsService     = mock[EclReturnsService]
  val mockEclReturnsConnector: EclReturnsConnector = mock[EclReturnsConnector]

  val view: StartAmendView                                 = app.injector.instanceOf[StartAmendView]
  val noObligationForPeriodView: NoObligationForPeriodView = app.injector.instanceOf[NoObligationForPeriodView]

  val controller = new StartAmendController(
    controllerComponents = mcc,
    authorise = fakeAuthorisedAction(internalId),
    eclAccountConnector = mockEclAccountConnector,
    eclReturnsService = mockEclReturnsService,
    eclReturnsConnector = mockEclReturnsConnector,
    noObligationForPeriodView = noObligationForPeriodView,
    view = view
  )

  "onPageLoad" should {
    "upsert the return data to contain all of the necessary information when periodKey matches obligation " +
      "and return Ok with the start amend view" in forAll {
        (internalId: String, obligationDetails: ObligationDetails, returnNumber: String) =>
          val openObligation = obligationDetails.copy(status = Open)
          val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))
          val updatedReturn  = EclReturn
            .empty(internalId = internalId, returnType = Some(AmendReturn))
            .copy(obligationDetails = Some(obligationDetails))

          when(mockEclAccountConnector.getObligations()(any()))
            .thenReturn(Future.successful(Some(obligationData)))

          when(mockEclReturnsService.getOrCreateReturn(any(), any())(any(), any()))
            .thenReturn(Future.successful(EclReturn.empty(internalId = internalId, returnType = Some(AmendReturn))))

          when(mockEclReturnsService.upsertEclReturn(any(), any())(any()))
            .thenReturn(Future.successful(EclReturn.empty(internalId = internalId, returnType = Some(AmendReturn))))

          when(mockEclReturnsConnector.upsertReturn(any())(any()))
            .thenReturn(Future.successful(updatedReturn))

          val result: Future[Result] =
            controller.onPageLoad(periodKey = openObligation.periodKey, returnNumber = returnNumber)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            returnNumber,
            openObligation.inboundCorrespondenceFromDate,
            openObligation.inboundCorrespondenceToDate
          )(fakeRequest, messages).toString()

          verify(mockEclReturnsService, times(1))
            .upsertEclReturn(any(), any())(any())

          reset(mockEclReturnsService)
      }
    "return No Obligation view when there is no obligation returned" in forAll {
      (periodKey: String, returnNumber: String) =>
        when(mockEclAccountConnector.getObligations()(any()))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = controller.onPageLoad(periodKey, returnNumber)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe noObligationForPeriodView()(fakeRequest, messages).toString()
    }
  }
}
