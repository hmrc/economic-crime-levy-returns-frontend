package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode}

class AmendReasonISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmendReasonController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.AmendReasonController.onPageLoad(NormalMode))

    "respond with 200 status and the amendment reason HTML view" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]

      stubGetReturn(eclReturn)
      stubGetSessionEmpty()

      val result = callRoute(FakeRequest(routes.AmendReasonController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Why are you requesting to amend your return?")
    }
  }

  s"POST ${routes.AmendReasonController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.AmendReasonController.onSubmit(NormalMode))

    "save the provided reason then redirect to the contact role page" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]
      val reason    = nonEmptyString.sample.get.trim

      stubGetReturn(eclReturn)
      stubGetSessionEmpty()

      val updatedReturn = eclReturn.copy(amendReason = Some(reason))

      stubUpsertReturn(updatedReturn)

      val result = callRoute(
        FakeRequest(routes.AmendReasonController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", reason))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.RelevantAp12MonthsController.onPageLoad(NormalMode).url)
    }
  }
}
