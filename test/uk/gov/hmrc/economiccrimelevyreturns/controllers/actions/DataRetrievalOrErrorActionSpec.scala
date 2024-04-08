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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.HeaderNames
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, EclReturn, FirstTimeReturn, ObligationData, SessionKeys}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataHandlingError, EclAccountError, SessionError}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.{AuthorisedRequest, ReturnDataRequest}
import uk.gov.hmrc.economiccrimelevyreturns.services.{EclAccountService, ReturnsService, SessionService}
import uk.gov.hmrc.http.HttpVerbs.GET

import scala.concurrent.Future

class DataRetrievalOrErrorActionSpec extends SpecBase {

  val mockEclReturnService: ReturnsService     = mock[ReturnsService]
  val mockSessionService: SessionService       = mock[SessionService]
  val mockEclAccountService: EclAccountService = mock[EclAccountService]

  class TestDataRetrievalOrErrorAction
      extends ReturnDataRetrievalOrErrorAction(mockEclReturnService, mockSessionService, mockEclAccountService) {
    override def refine[A](request: AuthorisedRequest[A]): Future[Either[Result, ReturnDataRequest[A]]] =
      super.refine(request)
  }

  val dataRetrievalOrErrorAction =
    new TestDataRetrievalOrErrorAction

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  private def getRedirectUrl(request: AuthorisedRequest[_]) =
    request.uri match {
      case amendUrl if amendUrl == routes.CheckYourAnswersController.onPageLoad(AmendReturn).url =>
        routes.NotableErrorController.returnAmendmentAlreadyRequested()
      case _                                                                                     => routes.NotableErrorController.eclReturnAlreadySubmitted()
    }

  "refine" should {
    "refine an AuthorisedRequest into a ReturnDataRequest" in forAll {
      eclReturn: EclReturn => (internalId: String, eclReferenceNumber: String) =>
        when(mockEclReturnService.getReturn(any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Option[EclReturn]](Future.successful(Right(Some(eclReturn)))))

        when(mockSessionService.get(any(), any(), any())(any()))
          .thenReturn(EitherT.rightT(alphaNumericString))

        val result: Future[Either[Result, ReturnDataRequest[AnyContentAsEmpty.type]]] =
          dataRetrievalOrErrorAction.refine(AuthorisedRequest(fakeRequest, internalId, eclReferenceNumber))

        await(result) shouldBe ReturnDataRequest(fakeRequest, internalId, eclReturn, None, eclReferenceNumber, None)
    }
    "refine an AuthorisedRequest into a ReturnDataRequest when no obligation details is retrived" in forAll {
      (eclReturn: EclReturn, internalId: String, eclReferenceNumber: String, obligationData: ObligationData) =>
        val eclReturnWithNoObligation = eclReturn.copy(obligationDetails = None)

        when(mockEclReturnService.getReturn(any())(any()))
          .thenReturn(
            EitherT[Future, DataHandlingError, Option[EclReturn]](
              Future.successful(Right(Some(eclReturnWithNoObligation)))
            )
          )

        when(mockSessionService.getOptional(any(), any(), any())(any()))
          .thenReturn(EitherT.rightT(Some(alphaNumericString)))

        when(mockSessionService.get(any(), any(), ArgumentMatchers.eq(SessionKeys.periodKey))(any()))
          .thenReturn(EitherT.rightT(alphaNumericString))

        when(mockEclAccountService.retrieveObligationData(any())).thenReturn(
          EitherT[Future, EclAccountError, Option[ObligationData]](Future.successful(Right(Some(obligationData))))
        )

        val result: Future[Either[Result, ReturnDataRequest[AnyContentAsEmpty.type]]] =
          dataRetrievalOrErrorAction.refine(AuthorisedRequest(fakeRequest, internalId, eclReferenceNumber))

        val act = await(result)
        act.isRight shouldBe true
    }

    "refine returns a failed exception" in forAll { (internalId: String, eclReferenceNumber: String) =>
      when(mockEclReturnService.getReturn(any())(any()))
        .thenReturn(
          EitherT.leftT[Future, Option[EclReturn]](
            DataHandlingError.BadGateway("ErrorMessage", INTERNAL_SERVER_ERROR)
          )
        )

      val result = intercept[Exception] {
        await(dataRetrievalOrErrorAction.refine(AuthorisedRequest(fakeRequest, internalId, eclReferenceNumber)))
      }

      result.getMessage shouldBe "ErrorMessage"

    }

    "return internal server error if retrive obligation data fails" in forAll {
      eclReturn: EclReturn => (internalId: String, eclReferenceNumber: String) =>
        when(mockEclReturnService.getReturn(any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Option[EclReturn]](Future.successful(Right(Some(eclReturn)))))

        val error = "Internal server error"
        when(mockSessionService.get(any(), any(), any())(any()))
          .thenReturn(
            EitherT[Future, SessionError, String](
              Future.successful(Left(SessionError.InternalUnexpectedError(error, None)))
            )
          )

        val result = intercept[Exception] {
          await(
            dataRetrievalOrErrorAction.refine(
              AuthorisedRequest(fakeRequest, internalId, eclReferenceNumber)
            )
          )
        }

        result.getMessage shouldBe error
    }

    "when period key is present in url is is extracted from the url rather than session" in forAll {
      eclReturn: EclReturn => (internalId: String, eclReferenceNumber: String, periodKey: String) =>
        when(mockEclReturnService.getReturn(any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Option[EclReturn]](Future.successful(Right(Some(eclReturn)))))

        val fakeRequest = FakeRequest(
          method = "GET",
          uri = s"/submit-economic-crime-levy-return/period/$periodKey",
          headers = FakeHeaders(Seq(HeaderNames.HOST -> "localhost")),
          body = AnyContentAsEmpty
        )

        val result: Future[Either[Result, ReturnDataRequest[AnyContentAsEmpty.type]]] =
          dataRetrievalOrErrorAction.refine(AuthorisedRequest(fakeRequest, internalId, eclReferenceNumber))

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
        when(mockEclReturnService.getReturn(any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Option[EclReturn]](Future.successful(Right(Some(eclReturn)))))

        when(mockSessionService.get(any(), any(), any())(any()))
          .thenReturn(EitherT.rightT(periodKey))

        val result: Future[Either[Result, ReturnDataRequest[AnyContentAsEmpty.type]]] =
          dataRetrievalOrErrorAction.refine(AuthorisedRequest(fakeRequest, internalId, eclReferenceNumber))

        await(result) shouldBe ReturnDataRequest(
          fakeRequest,
          internalId,
          eclReturn,
          None,
          eclReferenceNumber,
          Some(periodKey)
        )
    }

    "redirect to First time return error page when data retrieval fails" in forAll {
      (internalId: String, eclReferenceNumber: String, periodKey: String) =>
        when(mockEclReturnService.getReturn(any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Option[EclReturn]](Future.successful(Right(None))))

        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(FirstTimeReturn).url)

        val authorisedRequest = AuthorisedRequest(request, internalId, "ECLRefNumber12345")

        val result: Future[Either[Result, ReturnDataRequest[AnyContentAsEmpty.type]]] =
          dataRetrievalOrErrorAction.refine(authorisedRequest)

        await(result) shouldBe Left(Redirect(getRedirectUrl(authorisedRequest)))
    }

    "redirect to Amendment Return error page when data retrieval fails" in forAll {
      (internalId: String, eclReferenceNumber: String, periodKey: String) =>
        when(mockEclReturnService.getReturn(any())(any()))
          .thenReturn(EitherT[Future, DataHandlingError, Option[EclReturn]](Future.successful(Right(None))))

        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(AmendReturn).url)

        val authorisedRequest = AuthorisedRequest(request, internalId, "ECLRefNumber12345")

        val result: Future[Either[Result, ReturnDataRequest[AnyContentAsEmpty.type]]] =
          dataRetrievalOrErrorAction.refine(authorisedRequest)

        await(result) shouldBe Left(Redirect(getRedirectUrl(authorisedRequest)))
    }
  }

}
