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

package uk.gov.hmrc.economiccrimelevyreturns.connectors

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.http.Status.OK
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class BaseConnectorSpec extends SpecBase {

  case class Data(test: String)

  object Data {
    implicit val format: OFormat[Data] = Json.format[Data]
  }

  class TestConnector extends BaseConnector {
    private def response(valid: Boolean) = {
      val text = random[String]
      HttpResponse(
        OK,
        valid match {
          case true  => Json.toJson(Data(text)).toString()
          case false => text
        }
      )
    }

    def as(valid: Boolean): Future[Data] =
      response(valid).as[Data]

    def asOption(valid: Boolean): Future[Option[Data]] =
      response(valid).asOption[Data]
  }

  val connector = new TestConnector

  def test(future: Boolean => Future[_], valid: Boolean) =
    Try {
      future(valid).onComplete {
        case Success(_) if !valid => fail
        case Failure(_) if valid  => fail
      }
    } match {
      case Success(_) =>
      case Failure(_) => if (valid) fail
    }

  "as" should {
    "behave as expected" in forAll { valid: Boolean =>
      test(connector.as, valid)
    }
  }

  "asOption" should {
    "behave as expected" in forAll { valid: Boolean =>
      test(connector.asOption, valid)
    }
  }
}
