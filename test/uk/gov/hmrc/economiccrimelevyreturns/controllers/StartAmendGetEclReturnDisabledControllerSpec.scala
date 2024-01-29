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

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, EclAccountError}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclAccountService, EclCalculatorService, ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{NoObligationForPeriodView, StartAmendView}

import scala.concurrent.Future
import scala.concurrent.Future.unit

class StartAmendGetEclReturnDisabledControllerSpec extends SpecBase {

  override def configOverrides: Map[String, Any] = Map("features.getEclReturnEnabled" -> "false")

  val mockEclAccountService: EclAccountService      = mock[EclAccountService]
  val mockEclReturnsService: ReturnsService         = mock[ReturnsService]
  val mockSessionService: SessionService            = mock[SessionService]
  val mockEclLiabilityService: EclCalculatorService = mock[EclCalculatorService]

  val view: StartAmendView                                 = app.injector.instanceOf[StartAmendView]
  val noObligationForPeriodView: NoObligationForPeriodView = app.injector.instanceOf[NoObligationForPeriodView]

  val controller = new StartAmendController(
    controllerComponents = mcc,
    authorise = fakeAuthorisedAction(internalId),
    eclAccountService = mockEclAccountService,
    returnsService = mockEclReturnsService,
    sessionService = mockSessionService,
    noObligationForPeriodView = noObligationForPeriodView,
    view = view,
    appConfig,
    mockEclLiabilityService
  )

  "onPageLoad" should {
    "upsert the return data to contain all of the necessary information when periodKey matches obligation " +
      "and return Ok with the start amend view" in forAll {
        (internalId: String, obligationDetails: ObligationDetails, returnNumber: String) =>
          val openObligation = obligationDetails.copy(status = Open)
          val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))

          when(mockEclAccountService.retrieveObligationData(any()))
            .thenReturn(
              EitherT[Future, EclAccountError, Option[ObligationData]](Future.successful(Right(Some(obligationData))))
            )

          when(mockEclReturnsService.getOrCreateReturn(any(), any())(any(), any()))
            .thenReturn(
              EitherT[Future, DataHandlingError, EclReturn](
                Future.successful(Right(EclReturn.empty(internalId = internalId, returnType = Some(AmendReturn))))
              )
            )

          when(mockEclReturnsService.upsertReturn(any())(any()))
            .thenReturn(EitherT[Future, DataHandlingError, Unit](Future.successful(Right(()))))

          when(mockSessionService.upsert(any())(any()))
            .thenReturn(unit)

          val result: Future[Result] =
            controller.onPageLoad(periodKey = openObligation.periodKey, returnNumber = returnNumber)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            returnNumber,
            openObligation.inboundCorrespondenceFromDate,
            openObligation.inboundCorrespondenceToDate,
            Some(
              routes.StartAmendController
                .onPageLoad(periodKey = openObligation.periodKey, returnNumber = returnNumber)
                .url
            )
          )(fakeRequest, messages).toString()

          reset(mockEclReturnsService)
      }

    "return No Obligation view when there is no obligation returned" in forAll {
      (periodKey: String, returnNumber: String) =>
        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT[Future, EclAccountError, Option[ObligationData]](Future.successful(Right(None))))

        when(mockSessionService.upsert(any())(any()))
          .thenReturn(unit)

        val result: Future[Result] = controller.onPageLoad(periodKey, returnNumber)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe noObligationForPeriodView()(fakeRequest, messages).toString()
    }
  }
}
