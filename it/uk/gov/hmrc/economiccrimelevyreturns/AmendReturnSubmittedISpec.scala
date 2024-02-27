package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues.EmailMaxLength
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, GetSubscriptionResponse, ObligationDetails, SessionData, SessionKeys}

class AmendReturnSubmittedISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmendReturnSubmittedController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.AmendReturnSubmittedController.onPageLoad())

    "return 200 status and the amend return submitted HTML view" in {
      stubAuthorised()

      val subscriptionResponse = random[GetSubscriptionResponse]
      val obligationDetails    = random[ObligationDetails]
      val email                = emailAddress(EmailMaxLength).sample.get
      val eclReturn            = random[EclReturn]
        .copy(contactEmailAddress = Some(email), obligationDetails = Some(obligationDetails))
      val sessionData          = random[SessionData]
      val validSessionData     = sessionData.copy(values = Map(SessionKeys.PeriodKey -> testPeriodKey))

      stubDeleteReturn()
      stubDeleteSession()
      stubGetSession(validSessionData)
      stubGetReturn(eclReturn)

      stubGetSubscription(subscriptionResponse, testEclRegistrationReference)

      val result = callRoute(
        FakeRequest(routes.AmendReturnSubmittedController.onPageLoad()).withSession(
          (SessionKeys.Email, email),
          (SessionKeys.ObligationDetails, Json.toJson(obligationDetails).toString())
        )
      )

      status(result) shouldBe OK

      html(result) should include("Economic Crime Levy return amendment requested")
    }
  }
}
