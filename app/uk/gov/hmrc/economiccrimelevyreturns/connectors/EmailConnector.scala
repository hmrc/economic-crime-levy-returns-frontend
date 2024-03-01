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

package uk.gov.hmrc.economiccrimelevyreturns.connectors

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.email.AmendReturnSubmittedRequest.AmendReturnTemplateId
import uk.gov.hmrc.economiccrimelevyreturns.models.email.ReturnSubmittedEmailRequest._
import uk.gov.hmrc.economiccrimelevyreturns.models.email._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit
  ec: ExecutionContext
) extends BaseConnector
    with Retries {

  private val sendEmailUrl = url"${appConfig.emailBaseUrl}/hmrc/email"

  def sendReturnSubmittedEmail(
    to: String,
    returnSubmittedEmailParameters: ReturnSubmittedEmailParameters
  )(implicit
    hc: HeaderCarrier
  ): Future[Unit] = {
    val body = ReturnSubmittedEmailRequest(
      to = Seq(to),
      parameters = returnSubmittedEmailParameters,
      templateId =
        if (returnSubmittedEmailParameters.chargeReference.isDefined) ReturnTemplateId else NilReturnTemplateId
    )

    retryFor[Unit]("HMRC Email service - initial return")(retryCondition) {
      httpClient
        .post(sendEmailUrl)
        .withBody(Json.toJson(body))
        .executeAndExpect(ACCEPTED)
    }
  }

  def sendAmendReturnSubmittedEmail(to: String, parameters: AmendReturnSubmittedParameters)(implicit
    hc: HeaderCarrier
  ): Future[Unit] = {
    val body = AmendReturnSubmittedRequest(
      to = Seq(to),
      templateId = AmendReturnTemplateId,
      parameters = parameters
    )

    retryFor[Unit]("HMRC Email service - amend return")(retryCondition) {
      httpClient.post(url"$sendEmailUrl").withBody(Json.toJson(body)).executeAndExpect(ACCEPTED)
    }
  }
}
