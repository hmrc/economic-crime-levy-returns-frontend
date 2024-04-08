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

package uk.gov.hmrc.economiccrimelevyreturns.base

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import play.api.test.Helpers.{stubBodyParser, stubControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.EclTestData
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{FakeAuthorisedAction, FakeDataRetrievalAction, FakeDataRetrievalOrErrorAction, FakeNoOpStoreUrlAction}
import uk.gov.hmrc.economiccrimelevyreturns.generators.Generators
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.views.html.ErrorTemplate
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait SpecBase
    extends AnyWordSpec
    with Matchers
    with TryValues
    with OptionValues
    with DefaultAwaitTimeout
    with FutureAwaits
    with Results
    with GuiceOneAppPerSuite
    with MockitoSugar
    with BeforeAndAfterEach
    with ScalaCheckPropertyChecks
    with EclTestData
    with Generators {

  def configOverrides: Map[String, Any] = Map()

  val additionalAppConfig: Map[String, Any] = Map(
    "metrics.enabled"              -> false,
    "auditing.enabled"             -> false,
    "http-verbs.retries.intervals" -> List("1ms", "1ms", "1ms")
  ) ++ configOverrides

  val internalId: String                               = "test-internal-id"
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val appConfig: AppConfig                             = app.injector.instanceOf[AppConfig]
  val messagesApi: MessagesApi                         = app.injector.instanceOf[MessagesApi]
  val messages: Messages                               = messagesApi.preferred(fakeRequest)
  val bodyParsers: PlayBodyParsers                     = app.injector.instanceOf[PlayBodyParsers]
  val eclRegistrationReference: String                 = "test-ecl-registration-reference"
  val eclReturnReference: String                       = "test-ecl-return-reference"
  val config: Config                                   = app.injector.instanceOf[Config]
  val actorSystem: ActorSystem                         = ActorSystem("actor")
  val periodKey: String                                = "22XY"
  val fakeNoOpStoreUrlAction: FakeNoOpStoreUrlAction   = app.injector.instanceOf[FakeNoOpStoreUrlAction]
  implicit val errorTemplate: ErrorTemplate            = app.injector.instanceOf[ErrorTemplate]

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(additionalAppConfig)
      .build()

  def fakeAuthorisedAction(internalId: String) = new FakeAuthorisedAction(internalId, bodyParsers)

  def fakeDataRetrievalAction(data: EclReturn, periodKey: Option[String] = None) =
    new FakeDataRetrievalAction(data, periodKey)

  def fakeDataRetrievalOrErrorAction(
    data: EclReturn,
    periodKey: Option[String] = None,
    dataRetrievalFailure: Boolean = false
  ) =
    new FakeDataRetrievalOrErrorAction(data, periodKey, dataRetrievalFailure)

  def onwardRoute: Call = Call("GET", "/foo")

  val mcc: DefaultMessagesControllerComponents = {
    val stub = stubControllerComponents()
    DefaultMessagesControllerComponents(
      new DefaultMessagesActionBuilderImpl(stubBodyParser(AnyContentAsEmpty), stub.messagesApi)(stub.executionContext),
      DefaultActionBuilder(stub.actionBuilder.parser)(stub.executionContext),
      stub.parsers,
      messagesApi,
      stub.langs,
      stub.fileMimeTypes,
      stub.executionContext
    )
  }

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  def clearContact(eclReturn: EclReturn): EclReturn =
    eclReturn.copy(
      contactName = None,
      contactRole = None,
      contactEmailAddress = None,
      contactTelephoneNumber = None
    )
}
