/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.data.EitherT
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.SessionError
import uk.gov.hmrc.economiccrimelevyreturns.services.SessionService
import uk.gov.hmrc.http.HttpVerbs.GET

import scala.concurrent.Future

class StoreUrlActionSpec extends SpecBase {

  val mockSessionService: SessionService = mock[SessionService]

  class TestStoreUrlAction extends StoreUrlAction(mockSessionService) {
    override def refine[A](request: ReturnDataRequest[A]): Future[Either[Result, ReturnDataRequest[A]]] =
      super.refine(request)
  }

  val storeUrlAction = new TestStoreUrlAction

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  "refine" should {
    "store given url if Return type is FirstTimeReturn" in forAll { (eclReturn: EclReturn, url: String) =>
      val sessionData = SessionData(eclReturn.internalId, Map(SessionKeys.urlToReturnTo -> url))

      when(mockSessionService.upsert(ArgumentMatchers.eq(sessionData))(any()))
        .thenReturn(EitherT.fromEither[Future](Right(())))

      val request = FakeRequest(GET, url)

      await(
        storeUrlAction.refine(
          ReturnDataRequest(
            request,
            eclReturn.internalId,
            eclReturn.copy(returnType = Some(FirstTimeReturn)),
            None,
            testEclRegistrationReference,
            Some(testPeriodKey)
          )
        )
      )

      verify(mockSessionService, times(1)).upsert(any())(any())

      reset(mockSessionService)
    }

    "does not store given url if Return type is not FirstTimeReturn" in forAll(
      Arbitrary.arbitrary[EclReturn],
      Arbitrary.arbitrary[String],
      Arbitrary.arbitrary[ReturnType].retryUntil(_ != FirstTimeReturn)
    ) { (eclReturn: EclReturn, url: String, returnType: ReturnType) =>
      when(mockSessionService.upsert(any())(any()))
        .thenReturn(EitherT.fromEither[Future](Right(())))

      val request = FakeRequest(GET, url)

      await(
        storeUrlAction.refine(
          ReturnDataRequest(
            request,
            eclReturn.internalId,
            eclReturn.copy(returnType = Some(returnType)),
            None,
            testEclRegistrationReference,
            Some(testPeriodKey)
          )
        )
      )

      verify(mockSessionService, times(0)).upsert(any())(any())

      reset(mockSessionService)
    }
  }

  "refine returns a failed exception" in forAll { (eclReturn: EclReturn, url: String) =>
    val sessionData = SessionData(eclReturn.internalId, Map(SessionKeys.urlToReturnTo -> url))

    when(mockSessionService.upsert(ArgumentMatchers.eq(sessionData))(any()))
      .thenReturn(
        EitherT.leftT[Future, Unit](
          SessionError.BadGateway("ErrorMessage", INTERNAL_SERVER_ERROR)
        )
      )

    val request = FakeRequest(GET, url)

    val result = intercept[Exception] {
      await(
        storeUrlAction.refine(
          ReturnDataRequest(
            request,
            eclReturn.internalId,
            eclReturn.copy(returnType = Some(FirstTimeReturn)),
            None,
            testEclRegistrationReference,
            Some(testPeriodKey)
          )
        )
      )
    }

    result.getMessage shouldBe "ErrorMessage"

    reset(mockSessionService)
  }

}
