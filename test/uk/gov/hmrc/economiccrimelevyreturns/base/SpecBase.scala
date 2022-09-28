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

package uk.gov.hmrc.economiccrimelevyreturns.base

import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import play.api.test.Helpers.{stubBodyParser, stubControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.EclTestData
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{FakeAuthorisedAction, FakeDataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.navigation.FakeNavigator
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
    with EclTestData {

  val internalId: String                                     = "test-id"
  val emptyReturn: EclReturn                                 = EclReturn(internalId)
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type]       = FakeRequest()
  val appConfig: AppConfig                                   = app.injector.instanceOf[AppConfig]
  val messagesApi: MessagesApi                               = app.injector.instanceOf[MessagesApi]
  val messages: Messages                                     = messagesApi.preferred(fakeRequest)
  val bodyParsers: PlayBodyParsers                           = app.injector.instanceOf[PlayBodyParsers]
  val fakeAuthorisedAction                                   = new FakeAuthorisedAction(bodyParsers)
  def fakeDataRetrievalAction(data: EclReturn = emptyReturn) = new FakeDataRetrievalAction(data)

  def onwardRoute: Call = Call("GET", "/foo")

  val fakeNavigator = new FakeNavigator(onwardRoute)

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

}
