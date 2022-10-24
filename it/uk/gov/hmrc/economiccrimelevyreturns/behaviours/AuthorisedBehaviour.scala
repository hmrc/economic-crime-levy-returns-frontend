package uk.gov.hmrc.economiccrimelevyreturns.behaviours

import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase

import scala.concurrent.Future

trait AuthorisedBehaviour {
  self: ISpecBase =>

  def authorisedActionRoute(call: Call): Unit =
    "go to the not enrolled page if the user doesn't have an ECL enrolment" in {
      stubInsufficientEnrolments()

      val result: Future[Result] = callRoute(FakeRequest(call))

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "User does not have an ECL enrolment"
    }

}
