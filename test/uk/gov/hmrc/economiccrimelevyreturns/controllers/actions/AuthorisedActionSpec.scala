/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.mvc.{BodyParsers, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyreturns.{EnrolmentsWithEcl, EnrolmentsWithoutEcl}
import uk.gov.hmrc.http.UnauthorizedException

import scala.concurrent.Future

class AuthorisedActionSpec extends SpecBase {

  val defaultBodyParser: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]
  val mockAuthConnector: AuthConnector       = mock[AuthConnector]

  val authorisedAction =
    new BaseAuthorisedAction(mockAuthConnector, appConfig, defaultBodyParser)

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  val expectedRetrievals: Retrieval[Option[String] ~ Enrolments] =
    Retrievals.internalId and Retrievals.authorisedEnrolments

  "invokeBlock" should {
    "execute the block and return the result if authorised" in forAll {
      (internalId: String, enrolmentsWithEcl: EnrolmentsWithEcl) =>
        when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
          .thenReturn(Future(Some(internalId) and enrolmentsWithEcl.enrolments))

        val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe "Test"
    }

    "redirect the user to sign in when there is no active session" in {
      List(BearerTokenExpired(), MissingBearerToken(), InvalidBearerToken(), SessionRecordNotFound()).foreach {
        exception =>
          when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.failed(exception))

          val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

          status(result)               shouldBe SEE_OTHER
          redirectLocation(result).value should startWith(appConfig.signInUrl)
      }
    }

    "redirect the user to the unauthorised page if there is an authorisation exception" in {
      List(
        InsufficientConfidenceLevel(),
        UnsupportedAffinityGroup(),
        UnsupportedCredentialRole(),
        UnsupportedAuthProvider(),
        IncorrectCredentialStrength(),
        InternalError()
      ).foreach { exception =>
        when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.failed(exception))

        val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.UnauthorisedController.onPageLoad().url
      }
    }

    "throw an UnauthorizedException if there is no internal id" in {
      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
        .thenReturn(Future(None and Enrolments(Set.empty)))

      val result = intercept[UnauthorizedException] {
        await(authorisedAction.invokeBlock(fakeRequest, testAction))
      }

      result.message shouldBe "Unable to retrieve internalId"
    }

    "redirect the user to the not enrolled page if they don't have the ECL enrolment" in {
      when(
        mockAuthConnector
          .authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "User does not have an ECL enrolment"
    }

    "throw an IllegalStateException when the ECL enrolment is not present in the set of authorised enrolments" in forAll {
      (internalId: String, enrolmentsWithoutEcl: EnrolmentsWithoutEcl) =>
        when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
          .thenReturn(Future(Some(internalId) and enrolmentsWithoutEcl.enrolments))

        val result = intercept[IllegalStateException] {
          await(authorisedAction.invokeBlock(fakeRequest, testAction))
        }

        result.getMessage shouldBe s"Enrolment not found with key ${EclEnrolment.ServiceName}"
    }

    "throw an IllegalStateException when the ECL enrolment is present but the identifier is not" in forAll {
      (internalId: String, enrolmentsWithEcl: EnrolmentsWithEcl) =>
        val eclEnrolmentWithoutIdentifiers =
          Enrolments(enrolmentsWithEcl.enrolments.enrolments.map(_.copy(identifiers = Seq.empty)))

        when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
          .thenReturn(Future(Some(internalId) and eclEnrolmentWithoutIdentifiers))

        val result = intercept[IllegalStateException] {
          await(authorisedAction.invokeBlock(fakeRequest, testAction))
        }

        result.getMessage shouldBe s"Identifier not found in enrolment with name ${EclEnrolment.IdentifierKey}"
    }
  }

}
