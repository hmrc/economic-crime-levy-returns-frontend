package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._

class RelevantAp12MonthsISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.RelevantAp12MonthsController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.RelevantAp12MonthsController.onPageLoad())

    "respond with 200 status and the relevant AP 12 months view" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]

      stubGetReturn(eclReturn)

      val result = callRoute(FakeRequest(routes.RelevantAp12MonthsController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("Is your relevant accounting period 12 months?")
    }
  }

  s"POST ${routes.RelevantAp12MonthsController.onSubmit().url}"  should {
    behave like authorisedActionRoute(routes.RelevantAp12MonthsController.onSubmit())

    "save the selected option then redirect to the UK revenue page when the Yes option is selected" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]

      stubGetReturn(eclReturn)

      val updatedEclReturn = eclReturn.copy(relevantAp12Months = Some(true))

      stubGetReturn(updatedEclReturn)

      //TODO Implement call and assertion when building the next page
    }

    "save the selected option then redirect to the relevant AP length page when the No option is selected" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]

      stubGetReturn(eclReturn)

      val updatedReturn = eclReturn.copy(relevantAp12Months = Some(false))

      stubUpsertReturn(updatedReturn)

      //TODO Implement call and assertion when building the next page
    }
  }
}
