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

package uk.gov.hmrc.economiccrimelevyreturns.services

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.mvc.Session
import uk.gov.hmrc.economiccrimelevyreturns.connectors.SessionDataConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionData
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.SessionError
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class SessionServiceSpec extends ServiceSpec {

  val mockSessionConnector: SessionDataConnector = mock[SessionDataConnector]
  val service                                    = new SessionService(
    mockSessionConnector
  )

  "get" should {
    "return data if present" in forAll(
      nonEmptyString,
      nonEmptyString,
      nonEmptyString
    ) { (id: String, key: String, value: String) =>
      when(mockSessionConnector.get(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.successful(SessionData(id, Map(key -> value))))

      await(service.get(new Session(), id, key).value) shouldBe Right(value)

      when(mockSessionConnector.get(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.successful(SessionData(id, Map())))

      await(service.get(new Session() ++ Map(key -> value), id, key).value) shouldBe Right(value)
    }

    "return error if key not present" in forAll(
      nonEmptyString,
      nonEmptyString,
      Arbitrary.arbitrary[Boolean]
    ) { (id: String, key: String, is5xxError: Boolean) =>
      val code = getErrorCode(is5xxError)

      when(mockSessionConnector.get(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.successful(SessionData(id, Map())))

      await(service.get(new Session(), id, key).value) shouldBe Left(SessionError.KeyNotFound(key))

      when(mockSessionConnector.get(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.failed(testException))

      await(service.get(new Session(), id, key).value) shouldBe
        Left(SessionError.InternalUnexpectedError(testException.getMessage, Some(testException)))

      when(mockSessionConnector.get(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(code.toString, code)))

      await(service.get(new Session(), id, key).value) shouldBe
        Left(SessionError.BadGateway(code.toString, code))
    }
  }

  "getOptional" should {
    "return data if present" in forAll(
      nonEmptyString,
      nonEmptyString,
      nonEmptyString
    ) { (id: String, key: String, value: String) =>
      when(mockSessionConnector.get(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.successful(SessionData(id, Map(key -> value))))

      await(service.getOptional(new Session(), id, key).value) shouldBe Right(Some(value))

      when(mockSessionConnector.get(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.successful(SessionData(id, Map())))

      await(service.getOptional(new Session() ++ Map(key -> value), id, key).value) shouldBe Right(Some(value))
    }

    "return None if key not present" in forAll(
      nonEmptyString,
      nonEmptyString
    ) { (id: String, key: String) =>
      when(mockSessionConnector.get(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.successful(SessionData(id, Map())))

      await(service.getOptional(new Session(), id, key).value) shouldBe Right(None)
    }
  }

  "upsert" should {
    "merge existing data with new data and save result" in forAll(
      nonEmptyString,
      nonEmptyString,
      nonEmptyString
    ) { (id: String, key: String, value: String) =>
      val oldSessionData    = SessionData(id, Map(key -> value))
      val newSessionData    = SessionData(id, Map(s"$key$key" -> s"$value$value"))
      val mergedSessionData = SessionData(
        internalId = id,
        values = oldSessionData.values ++ newSessionData.values
      )

      when(mockSessionConnector.get(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.successful(oldSessionData))

      when(mockSessionConnector.upsert(ArgumentMatchers.eq(mergedSessionData))(any()))
        .thenReturn(Future.successful())

      await(service.upsert(newSessionData).value) shouldBe Right()
    }

    "return an error if failure" in forAll(
      nonEmptyString,
      nonEmptyString,
      nonEmptyString,
      Arbitrary.arbitrary[Boolean]
    ) { (id: String, key: String, value: String, is5xxError: Boolean) =>
      val code = getErrorCode(is5xxError)

      val oldSessionData    = SessionData(id, Map(key -> value))
      val newSessionData    = SessionData(id, Map(s"$key$key" -> s"$value$value"))
      val mergedSessionData = SessionData(
        internalId = id,
        values = oldSessionData.values ++ newSessionData.values
      )

      when(mockSessionConnector.get(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.successful(oldSessionData))

      when(mockSessionConnector.upsert(ArgumentMatchers.eq(mergedSessionData))(any()))
        .thenReturn(Future.failed(testException))

      await(service.upsert(newSessionData).value) shouldBe
        Left(SessionError.InternalUnexpectedError(testException.getMessage, Some(testException)))

      when(mockSessionConnector.upsert(ArgumentMatchers.eq(mergedSessionData))(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(code.toString, code)))

      await(service.upsert(newSessionData).value) shouldBe
        Left(SessionError.BadGateway(code.toString, code))
    }
  }

  "delete" should {
    "delete the session with given id" in forAll(
      nonEmptyString
    ) { id: String =>
      when(mockSessionConnector.delete(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.successful())

      await(service.delete(id).value) shouldBe Right()
    }

    "return an error if failure" in forAll(
      nonEmptyString,
      Arbitrary.arbitrary[Boolean]
    ) { (id: String, is5xxError: Boolean) =>
      val code = getErrorCode(is5xxError)

      when(mockSessionConnector.delete(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.failed(testException))

      await(service.delete(id).value) shouldBe
        Left(SessionError.InternalUnexpectedError(testException.getMessage, Some(testException)))

      when(mockSessionConnector.delete(ArgumentMatchers.eq(id))(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(code.toString, code)))

      await(service.delete(id).value) shouldBe
        Left(SessionError.BadGateway(code.toString, code))
    }
  }
}
