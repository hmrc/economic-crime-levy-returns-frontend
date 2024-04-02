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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.KeyValue
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.{EclEnrolment, Enrolment, QueryKnownFactsResponse}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataHandlingError
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.LocalDate
import scala.concurrent.Future

class EnrolmentStoreProxyServiceSpec extends ServiceSpec {
  val mockEnrolmentStoreProxyConnector: EnrolmentStoreProxyConnector = mock[EnrolmentStoreProxyConnector]
  val service                                                        = new EnrolmentStoreProxyService(mockEnrolmentStoreProxyConnector)

  "getEclRegistrationDate" should {
    "return the ECL registration date from the query known facts response" in forAll {
      eclRegistrationReference: String =>
        val queryKnownFactsResponse = QueryKnownFactsResponse(
          service = EclEnrolment.serviceName,
          enrolments = Seq(
            Enrolment(
              identifiers = Seq(KeyValue(key = EclEnrolment.identifierKey, value = eclRegistrationReference)),
              verifiers = Seq(KeyValue(key = EclEnrolment.verifierKey, value = "20230131"))
            )
          )
        )

        when(mockEnrolmentStoreProxyConnector.queryKnownFacts(ArgumentMatchers.eq(eclRegistrationReference))(any()))
          .thenReturn(Future.successful(queryKnownFactsResponse))

        val result = await(service.getEclRegistrationDate(eclRegistrationReference).value)

        result shouldBe Right(LocalDate.parse("2023-01-31"))
    }

    "return InternalUnexpectedError if the ECL registration date could not be found in the enrolment" in forAll {
      eclRegistrationReference: String =>
        val queryKnownFactsResponse = QueryKnownFactsResponse(
          service = EclEnrolment.serviceName,
          enrolments = Seq.empty
        )

        when(mockEnrolmentStoreProxyConnector.queryKnownFacts(ArgumentMatchers.eq(eclRegistrationReference))(any()))
          .thenReturn(Future.successful(queryKnownFactsResponse))

        val result =
          await(service.getEclRegistrationDate(eclRegistrationReference).value)

        result shouldBe Left(
          DataHandlingError
            .InternalUnexpectedError(
              None,
              Some("Enrolment store: ECL registration date could not be found in the enrolment")
            )
        )
    }

    "return error if failure" in forAll(
      nonEmptyString,
      Arbitrary.arbitrary[Boolean]
    ) { (eclReference: String, is5xxError: Boolean) =>
      val code = getErrorCode(is5xxError)

      when(mockEnrolmentStoreProxyConnector.queryKnownFacts(ArgumentMatchers.eq(eclReference))(any()))
        .thenReturn(Future.failed(testException))

      await(service.getEclRegistrationDate(eclReference).value) shouldBe
        Left(DataHandlingError.InternalUnexpectedError(Some(testException)))

      when(mockEnrolmentStoreProxyConnector.queryKnownFacts(ArgumentMatchers.eq(eclReference))(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(code.toString, code)))

      await(service.getEclRegistrationDate(eclReference).value) shouldBe
        Left(DataHandlingError.BadGateway(s"Get ECL Registration Date Failed - ${code.toString}", code))
    }
  }

}
