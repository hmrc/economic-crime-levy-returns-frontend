package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{ObligationDetails, SessionKeys}

class AmendReturnSubmittedISpec extends ISpecBase with AuthorisedBehaviour{

  s"GET ${routes.AmendReturnSubmittedController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.AmendReturnSubmittedController.onPageLoad())

    "return 200 status and the amend return submitted HTML view" in {
      stubAuthorised()

      val obligationDetails = random[ObligationDetails]
      val email = random[String]

      val result = callRoute(FakeRequest(routes.AmendReturnSubmittedController.onPageLoad()).withSession(
        (SessionKeys.Email, email),
        (SessionKeys.ObligationDetails, Json.toJson(obligationDetails).toString())
      ))

      status(result) shouldBe OK

      html(result) should include("Economic Crime Levy return amended")
    }
  }
}
