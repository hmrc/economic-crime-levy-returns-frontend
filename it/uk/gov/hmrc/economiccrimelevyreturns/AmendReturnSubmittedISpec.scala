package uk.gov.hmrc.economiccrimelevyreturns

import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues.emailMaxLength
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{Band, EclReturn, GetSubscriptionResponse, ObligationDetails, SessionData, SessionKeys}
import org.scalacheck.Arbitrary.arbitrary

class AmendReturnSubmittedISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmendReturnSubmittedController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.AmendReturnSubmittedController.onPageLoad())

    "return 200 status and the amend return submitted HTML view" in {
      stubAuthorised()

      val subscriptionResponse = arbitrary[GetSubscriptionResponse].sample.get
      val obligationDetails    = arbitrary[ObligationDetails].sample.get
      val email                = emailAddress(emailMaxLength).sample.get
      val band                 = arbitrary[Band].sample.get
      val amountDue            = arbitrary[Int].sample.get
      val isIncrease           = arbitrary[Boolean].sample.get
      val eclReturn            = arbitrary[EclReturn].sample.get
        .copy(contactEmailAddress = Some(email), obligationDetails = Some(obligationDetails))
      val sessionData          = arbitrary[SessionData].sample.get
      val validSessionData     = sessionData.copy(values = Map(SessionKeys.periodKey -> testPeriodKey))

      stubDeleteReturn()
      stubDeleteSession()
      stubGetSession(validSessionData)
      stubGetReturn(eclReturn)

      stubGetSubscription(subscriptionResponse, testEclRegistrationReference)

      val result = callRoute(
        FakeRequest(routes.AmendReturnSubmittedController.onPageLoad()).withSession(
          (SessionKeys.email, email),
          (SessionKeys.band, band.toString),
          (SessionKeys.amountDue, amountDue.toString),
          (SessionKeys.isIncrease, isIncrease.toString),
          (SessionKeys.obligationDetails, Json.toJson(obligationDetails).toString()),
          (SessionKeys.startAmendUrl, routes.StartAmendController.onPageLoad(testPeriodKey, eclReturn.internalId).url)
        )
      )

      status(result) shouldBe OK

      html(result) should include("Economic Crime Levy return amendment requested")
      html(result) should include("Amending your Economic Crime Levy return")
    }
  }
}
