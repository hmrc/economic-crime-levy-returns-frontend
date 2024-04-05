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

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import play.api.http.HeaderNames
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataHandlingError
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.{AuthorisedRequest, ReturnDataRequest}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclAccountService, ReturnsService, SessionService}

import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase {

  val mockEclReturnService: ReturnsService     = mock[ReturnsService]
  val mockSessionService: SessionService       = mock[SessionService]
  val mockEclAccountService: EclAccountService = mock[EclAccountService]

  class TestDataRetrievalAction
      extends ReturnDataRetrievalAction(mockEclReturnService, mockSessionService, mockEclAccountService) {
    override def transform[A](request: AuthorisedRequest[A]): Future[ReturnDataRequest[A]] =
      super.transform(request)
  }

  val dataRetrievalAction =
    new TestDataRetrievalAction

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  "transform" should {
    "transform an AuthorisedRequest into a ReturnDataRequest" in forAll {
      eclReturn: EclReturn => (internalId: String, eclReferenceNumber: String) =>
        when(mockEclReturnService.getOrCreateReturn(any())(any(), any()))
          .thenReturn(EitherT[Future, DataHandlingError, EclReturn](Future.successful(Right(eclReturn))))

        when(mockSessionService.get(any(), any(), any())(any()))
          .thenReturn(EitherT.rightT(alphaNumericString))

        val result: Future[ReturnDataRequest[AnyContentAsEmpty.type]] =
          dataRetrievalAction.transform(AuthorisedRequest(fakeRequest, internalId, eclReferenceNumber))

        await(result) shouldBe ReturnDataRequest(fakeRequest, internalId, eclReturn, None, eclReferenceNumber, None)
    }

    "when period key is present in url is is extracted from the url rather than session" in forAll {
      eclReturn: EclReturn => (internalId: String, eclReferenceNumber: String, periodKey: String) =>
        when(mockEclReturnService.getOrCreateReturn(any())(any(), any()))
          .thenReturn(EitherT[Future, DataHandlingError, EclReturn](Future.successful(Right(eclReturn))))

        val fakeRequest = FakeRequest(
          method = "GET",
          uri = s"/submit-economic-crime-levy-return/period/$periodKey",
          headers = FakeHeaders(Seq(HeaderNames.HOST -> "localhost")),
          body = AnyContentAsEmpty
        )

        val result: Future[ReturnDataRequest[AnyContentAsEmpty.type]] =
          dataRetrievalAction.transform(AuthorisedRequest(fakeRequest, internalId, eclReferenceNumber))

        await(result) shouldBe ReturnDataRequest(
          fakeRequest,
          internalId,
          eclReturn,
          None,
          eclReferenceNumber,
          Some(periodKey)
        )
    }

    "when period key is not in url, it is extracted from the session" in forAll {
      eclReturn: EclReturn => (internalId: String, eclReferenceNumber: String, periodKey: String) =>
        when(mockEclReturnService.getOrCreateReturn(any())(any(), any()))
          .thenReturn(EitherT[Future, DataHandlingError, EclReturn](Future.successful(Right(eclReturn))))

        when(mockSessionService.get(any(), any(), any())(any()))
          .thenReturn(EitherT.rightT(periodKey))

        val result: Future[ReturnDataRequest[AnyContentAsEmpty.type]] =
          dataRetrievalAction.transform(AuthorisedRequest(fakeRequest, internalId, eclReferenceNumber))

        await(result) shouldBe ReturnDataRequest(
          fakeRequest,
          internalId,
          eclReturn,
          None,
          eclReferenceNumber,
          Some(periodKey)
        )
    }

    "transform returns a failed exception" in forAll { (internalId: String, eclReferenceNumber: String) =>
      when(mockEclReturnService.getOrCreateReturn(any(), any())(any(), any()))
        .thenReturn(
          EitherT.leftT[Future, EclReturn](
            DataHandlingError.BadGateway("ErrorMessage", INTERNAL_SERVER_ERROR)
          )
        )

      val result = intercept[Exception] {
        await(dataRetrievalAction.transform(AuthorisedRequest(fakeRequest, internalId, eclReferenceNumber)))
      }

      result.getMessage shouldBe "ErrorMessage"

    }

  }

}
