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

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.RequestHeader
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  private val contactHost                  = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "economic-crime-levy-returns-frontend"
  private val exitSurveyHost               = configuration.get[String]("feedback-frontend.host")
  private val exitSurveyServiceIdentifier  = configuration.get[String]("feedback-frontend.serviceId")

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${Redirect(host + request.uri)}"

  val amendReturnsEnabled: Boolean        = configuration.get[Boolean]("features.amendReturnsEnabled")
  val claimUrl: String                    = configuration.get[String]("urls.claim")
  val countdown: Int                      = configuration.get[Int]("timeout-dialog.countdown")
  val eclAccountBaseUrl: String           = servicesConfig.baseUrl("economic-crime-levy-account")
  val eclAccountUrl: String               = configuration.get[String]("urls.eclAccount")
  val eclCalculatorBaseUrl: String        = servicesConfig.baseUrl("economic-crime-levy-calculator")
  val eclRegistrationBaseUrl: String      = servicesConfig.baseUrl("economic-crime-levy-registration")
  val eclReturnsBaseUrl: String           = servicesConfig.baseUrl("economic-crime-levy-returns")
  val enrolmentStoreProxyBaseUrl: String  = servicesConfig.baseUrl("enrolment-store-proxy")
  val emailBaseUrl: String                = servicesConfig.baseUrl("email")
  val exitSurveyUrl: String               = s"$exitSurveyHost/feedback/$exitSurveyServiceIdentifier"
  val getEclReturnEnabled: Boolean        = configuration.get[Boolean]("features.getEclReturnEnabled")
  val getSubscriptionEnabled: Boolean     = configuration.get[Boolean]("features.getSubscriptionEnabled")
  val languageTranslationEnabled: Boolean = configuration.get[Boolean]("features.welsh-translation")
  val paymentsEnabled: Boolean            = configuration.get[Boolean]("features.paymentsEnabled")
  val signInUrl: String                   = configuration.get[String]("urls.signIn")
  val signOutUrl: String                  = configuration.get[String]("urls.signOut")
  val timeout: Int                        = configuration.get[Int]("timeout-dialog.timeout")
}
