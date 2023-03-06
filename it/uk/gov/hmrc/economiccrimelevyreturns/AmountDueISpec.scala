package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode}

class AmountDueISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmountDueController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.AmountDueController.onPageLoad())

    "respond with 200 status and the ECL amount due view when the ECL return data is valid" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]

      stubGetReturn(eclReturn)

      val result = callRoute(FakeRequest(routes.AmountDueController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("Amount of ECL you need to pay")
    }

    "redirect to the invalid data page when the ECL return data is invalid" in {
      stubAuthorised()

      val eclReturn = random[EclReturn].copy(calculatedLiability = None)

      stubGetReturn(eclReturn)

      val result = callRoute(FakeRequest(routes.AmountDueController.onPageLoad()))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
    }
  }

  s"POST ${routes.AmountDueController.onSubmit().url}"  should {
    behave like authorisedActionRoute(routes.AmountDueController.onSubmit())

    "redirect to the who is completing this return page" in {
      stubAuthorised()

      val eclReturn = random[ValidEclReturn].eclReturn

      stubGetReturn(eclReturn)

      val result = callRoute(FakeRequest(routes.AmountDueController.onSubmit()))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.ContactNameController.onPageLoad(NormalMode).url)
    }
  }

}
