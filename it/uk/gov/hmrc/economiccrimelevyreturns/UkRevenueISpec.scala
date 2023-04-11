package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculateLiabilityRequest, CalculatedLiability, EclReturn, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.models.Band._
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.utils.EclTaxYear

class UkRevenueISpec extends ISpecBase with AuthorisedBehaviour {

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

    "save the UK revenue then redirect to the AML regulated activity page" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]
      val ukRevenue = longsInRange(UkRevenueThreshold, MinMaxValues.RevenueMax).sample.get
      val calculatedLiability = random[CalculatedLiability].copy(calculatedBand = Large)

      stubGetReturn(eclReturn.copy(relevantAp12Months = Some(true), calculatedLiability = None))

      val updatedReturn = eclReturn.copy(
        relevantAp12Months = Some(true),
        relevantApRevenue = Some(ukRevenue),
        calculatedLiability = None
      )

      stubUpsertReturn(updatedReturn)
      stubCalculateLiability(CalculateLiabilityRequest(EclTaxYear.YearInDays, EclTaxYear.YearInDays, ukRevenue), calculatedLiability)
      stubUpsertReturn(updatedReturn.copy(calculatedLiability = Some(calculatedLiability)))

      val result = callRoute(
        FakeRequest(routes.UkRevenueController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", ukRevenue.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.AmlRegulatedActivityController.onPageLoad(NormalMode).url)
    }
  }

}
