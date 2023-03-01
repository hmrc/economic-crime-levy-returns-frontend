package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._

class RelevantAp12MonthsISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.RelevantAp12MonthsController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.RelevantAp12MonthsController.onPageLoad(NormalMode))

    "respond with 200 status and the relevant AP 12 months view" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]

      stubGetReturn(eclReturn)

      val result = callRoute(FakeRequest(routes.RelevantAp12MonthsController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Is your relevant accounting period 12 months?")
    }
  }

  s"POST ${routes.RelevantAp12MonthsController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.RelevantAp12MonthsController.onSubmit(NormalMode))

    "save the selected option then redirect to the UK revenue page when the Yes option is selected" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]

      stubGetReturn(eclReturn)

      val updatedReturn =
        eclReturn.copy(relevantAp12Months = Some(true), relevantApLength = None, calculatedLiability = None)

      stubUpsertReturn(updatedReturn)

      val result = callRoute(
        FakeRequest(routes.RelevantAp12MonthsController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.UkRevenueController.onPageLoad(NormalMode).url)
    }

    "save the selected option then redirect to the relevant AP length page when the No option is selected" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]

      stubGetReturn(eclReturn)

      val updatedReturn = eclReturn.copy(relevantAp12Months = Some(false), calculatedLiability = None)

      stubUpsertReturn(updatedReturn)

      val result = callRoute(
        FakeRequest(routes.RelevantAp12MonthsController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.RelevantApLengthController.onPageLoad(NormalMode).url)
    }
  }
}
