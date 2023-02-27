package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._

class UkRevenueISpec extends ISpecBase with AuthorisedBehaviour {

  val minRevenue = 0L
  val maxRevenue = 99999999999L

  val revenueGen: Gen[Long] = Gen.chooseNum[Long](minRevenue, maxRevenue)

  s"GET ${routes.UkRevenueController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.UkRevenueController.onPageLoad(NormalMode))

    "respond with 200 status and the UK revenue view" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]

      stubGetReturn(eclReturn)

      val result = callRoute(FakeRequest(routes.UkRevenueController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What was your UK revenue for the relevant accounting period?")
    }
  }

  s"POST ${routes.UkRevenueController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.UkRevenueController.onSubmit(NormalMode))

    "save the UK revenue then redirect to the ??? page" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]
      val ukRevenue    = revenueGen.sample.get

      stubGetReturn(eclReturn.copy(relevantAp12Months = Some(true)))

      val updatedReturn = eclReturn.copy(
        relevantAp12Months = Some(true),
        relevantApRevenue = Some(ukRevenue)
      )

      stubUpsertReturn(updatedReturn)

      //TODO Implement call and assertion when building the next page
    }
  }

}
