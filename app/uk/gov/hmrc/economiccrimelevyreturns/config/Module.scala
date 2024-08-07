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

package uk.gov.hmrc.economiccrimelevyreturns.config

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}
import uk.gov.hmrc.economiccrimelevyreturns.connectors.{EnrolmentStoreProxyConnector, EnrolmentStoreProxyConnectorImpl}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions._
import uk.gov.hmrc.economiccrimelevyreturns.testonly.connectors.stubs.StubEnrolmentStoreProxyConnector

import java.time.{Clock, ZoneOffset}

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[DataRetrievalAction])
      .to(classOf[ReturnDataRetrievalAction])
      .asEagerSingleton()
    bind(classOf[DataRetrievalOrErrorAction])
      .to(classOf[ReturnDataRetrievalOrErrorAction])
      .asEagerSingleton()
    bind(classOf[AuthorisedAction]).to(classOf[BaseAuthorisedAction]).asEagerSingleton()
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC))

    val enrolmentStoreProxyStubEnabled: Boolean = configuration.get[Boolean]("features.enrolmentStoreProxyStubEnabled")

    if (enrolmentStoreProxyStubEnabled) {
      bind(classOf[EnrolmentStoreProxyConnector])
        .to(classOf[StubEnrolmentStoreProxyConnector])
        .asEagerSingleton()
    } else {
      bind(classOf[EnrolmentStoreProxyConnector])
        .to(classOf[EnrolmentStoreProxyConnectorImpl])
        .asEagerSingleton()
    }
  }

}
