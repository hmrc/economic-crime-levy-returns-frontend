package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.Band._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculateLiabilityRequest, CalculatedLiability, EclReturn, NormalMode}

class UkRevenueISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.UkRevenueController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.UkRevenueController.onPageLoad(NormalMode))

    "respond with 200 status and the UK revenue view" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]

      stubGetReturn(eclReturn)
      stubGetSessionEmpty()

      val result = callRoute(FakeRequest(routes.UkRevenueController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Total UK revenue")
    }
  }

  s"POST ${routes.UkRevenueController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.UkRevenueController.onSubmit(NormalMode))

    "save the UK revenue then redirect to the AML regulated activity page when the calculated band size is not Small" in {
      stubAuthorised()

      val eclReturn           = random[EclReturn]
      val ukRevenue           = bigDecimalInRange(UkRevenueThreshold, MinMaxValues.RevenueMax.toDouble).sample.get
      val calculatedLiability = random[CalculatedLiability].copy(calculatedBand = Large)

      stubGetReturn(eclReturn.copy(relevantAp12Months = Some(true), calculatedLiability = None))
      stubGetSessionEmpty()

      val updatedReturn = eclReturn.copy(
        relevantAp12Months = Some(true),
        relevantApRevenue = Some(ukRevenue),
        carriedOutAmlRegulatedActivityForFullFy = None,
        calculatedLiability = None
      )

      stubUpsertReturn(updatedReturn)
      stubCalculateLiability(CalculateLiabilityRequest(FullYear, FullYear, ukRevenue.toLong), calculatedLiability)
      stubUpsertReturn(updatedReturn.copy(calculatedLiability = Some(calculatedLiability)))

      val result = callRoute(
        FakeRequest(routes.UkRevenueController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", ukRevenue.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.AmlRegulatedActivityController.onPageLoad(NormalMode).url)
    }

    "save the UK revenue then redirect to the ECL amount due page when the calculated band size is Small (nil return)" in {
      stubAuthorised()

      val eclReturn           = random[EclReturn]
      val ukRevenue           = bigDecimalInRange(MinMaxValues.RevenueMin.toDouble, UkRevenueThreshold).sample.get
      val calculatedLiability = random[CalculatedLiability].copy(calculatedBand = Small)

      stubGetReturn(eclReturn.copy(relevantAp12Months = Some(true), calculatedLiability = None)) test
        stubGetSessionEmpty()

      val updatedReturn = eclReturn.copy(
        relevantAp12Months = Some(true),
        relevantApRevenue = Some(ukRevenue),
        carriedOutAmlRegulatedActivityForFullFy = None,
        amlRegulatedActivityLength = None,
        calculatedLiability = None
      )

      stubUpsertReturn(updatedReturn)
      stubCalculateLiability(
        CalculateLiabilityRequest(FullYear, FullYear, ukRevenue.toLong),
        calculatedLiability
      )
      stubUpsertReturn(updatedReturn.copy(calculatedLiability = Some(calculatedLiability)))

      val result = callRoute(
        FakeRequest(routes.UkRevenueController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", ukRevenue.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.AmountDueController.onPageLoad(NormalMode).url)
    }
  }

}
