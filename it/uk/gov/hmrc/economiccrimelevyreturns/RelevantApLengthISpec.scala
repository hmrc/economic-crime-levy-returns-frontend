package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class RelevantApLengthISpec extends ISpecBase with AuthorisedBehaviour {

  val minDays = 1
  val maxDays = 999

  s"GET ${routes.RelevantApLengthController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.RelevantApLengthController.onPageLoad(NormalMode))

    "respond with 200 status and the relevant AP length view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.RelevantApLengthController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("How long is your relevant accounting period?")
    }
  }

  s"POST ${routes.RelevantApLengthController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.RelevantApLengthController.onSubmit(NormalMode))

    "save the relevant AP length then redirect to the UK revenue page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration     = random[Registration]
      val relevantApLength = Gen.chooseNum[Int](minDays, maxDays).sample.get

      stubGetRegistration(registration)

      val updatedRegistration =
        registration.copy(relevantApLength = Some(relevantApLength), revenueMeetsThreshold = None)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.RelevantApLengthController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", relevantApLength.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.UkRevenueController.onPageLoad(NormalMode).url)
    }
  }
}
