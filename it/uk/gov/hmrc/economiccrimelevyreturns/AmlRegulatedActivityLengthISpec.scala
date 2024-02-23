package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculateLiabilityRequest, CalculatedLiability, EclReturn, NormalMode, SessionData, SessionKeys}

class AmlRegulatedActivityLengthISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmlRegulatedActivityLengthController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.AmlRegulatedActivityLengthController.onPageLoad(NormalMode))

    "respond with 200 status and the AML regulated activity length view" in {
      stubAuthorised()

      val eclReturn        = random[EclReturn]
      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.PeriodKey -> testPeriodKey))

      stubGetReturn(eclReturn)
      stubGetSession(validSessionData)

      val result = callRoute(FakeRequest(routes.AmlRegulatedActivityLengthController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("How many days of the financial year did you carry out AML-regulated activity?")
    }
  }

  s"POST ${routes.AmlRegulatedActivityLengthController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.AmlRegulatedActivityLengthController.onSubmit(NormalMode))

    "save the relevant AML regulated activity length then redirect to the ECL amount due page" in {
      stubAuthorised()

      val ukRevenue                  = longsInRange(MinMaxValues.RevenueMin.toLong, MinMaxValues.RevenueMax.toLong).sample.get
      val amlRegulatedActivityLength = Gen.chooseNum[Int](MinMaxValues.AmlDaysMin, MinMaxValues.AmlDaysMax).sample.get
      val eclReturn                  = random[EclReturn].copy(
        relevantAp12Months = Some(true),
        relevantApRevenue = Some(ukRevenue),
        carriedOutAmlRegulatedActivityForFullFy = Some(false)
      )
      val calculatedLiability        = random[CalculatedLiability]
      val sessionData                = random[SessionData]
      val validSessionData           = sessionData.copy(values = Map(SessionKeys.PeriodKey -> testPeriodKey))

      stubGetReturn(eclReturn)
      stubGetSession(validSessionData)

      val updatedReturn =
        eclReturn.copy(amlRegulatedActivityLength = Some(amlRegulatedActivityLength), calculatedLiability = None)

      stubUpsertReturn(updatedReturn)
      stubCalculateLiability(
        CalculateLiabilityRequest(amlRegulatedActivityLength, FullYear, ukRevenue),
        calculatedLiability
      )

      val result = callRoute(
        FakeRequest(routes.AmlRegulatedActivityLengthController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", amlRegulatedActivityLength.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.AmountDueController.onPageLoad(NormalMode).url)
    }
  }
}
