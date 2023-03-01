package uk.gov.hmrc.economiccrimelevyreturns.behaviours

import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase

import scala.concurrent.Future

trait AuthorisedBehaviour {
  self: ISpecBase =>

  def authorisedActionRoute(call: Call): Unit =
    "authorisedAction" should {
      "go to the not enrolled page if the user doesn't have an ECL enrolment" in {
        stubInsufficientEnrolments()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.NotableErrorController.notRegistered().url
      }

      "go to the agent not supported page if the user has an agent affinity group" in {
        stubAuthorisedWithAgentAffinityGroup()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)          shouldBe OK
        contentAsString(result) shouldBe "Agent account not supported - must be an organisation or individual"
      }
    }

}
