package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculateLiabilityRequest, CalculatedLiability, CheckMode, EclReturn, NormalMode, SessionData, SessionKeys}

class AmlRegulatedActivityLengthISpec extends ISpecBase with AuthorisedBehaviour {

  val ukRevenue = random[BigDecimal]

  private def updateAmlActivityLength(eclReturn: EclReturn, length: Int) = {
    val updatedReturn       = eclReturn.copy(amlRegulatedActivityLength = Some(length))
    val calculatedLiability = random[CalculatedLiability]
    stubCalculateLiability(
      CalculateLiabilityRequest(length, FullYear, ukRevenue.longValue),
      calculatedLiability
    )
    updatedReturn.copy(calculatedLiability = Some(calculatedLiability))
  }

  private def clearAmlActivityLength(eclReturn: EclReturn) =
    eclReturn.copy(amlRegulatedActivityLength = None)

  private def testSetup(eclReturn: EclReturn = blankReturn, internalId: String = testInternalId): EclReturn = {
    stubGetSession(
      SessionData(
        internalId = internalId,
        values = Map(SessionKeys.PeriodKey -> testPeriodKey)
      )
    )

    val updatedReturn = eclReturn.copy(
      relevantAp12Months = Some(true),
      relevantApRevenue = Some(ukRevenue),
      carriedOutAmlRegulatedActivityForFullFy = Some(false),
      calculatedLiability = None
    )

    updateContactName(updatedReturn)
  }

  def validLength =
    Gen.chooseNum[Int](MinMaxValues.AmlDaysMin, MinMaxValues.AmlDaysMax).sample.get

  s"GET ${routes.AmlRegulatedActivityLengthController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.AmlRegulatedActivityLengthController.onPageLoad(NormalMode))

    "respond with 200 status and the AML regulated activity length view" in {
      stubAuthorised()

      stubGetReturn(testSetup(random[EclReturn]))
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.AmlRegulatedActivityLengthController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("How many days of the financial year did you carry out AML-regulated activity?")
    }
  }

  s"POST ${routes.AmlRegulatedActivityLengthController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.AmlRegulatedActivityLengthController.onSubmit(NormalMode))

    "save the relevant AML regulated activity length then redirect to the ECL amount due page" in {
      stubAuthorised()

      val amlActivityLength = validLength

      val eclReturn = testSetup(random[EclReturn])
      stubGetReturn(clearAmlActivityLength(eclReturn))
      stubUpsertReturn(updateAmlActivityLength(eclReturn, amlActivityLength))

      val result = callRoute(
        FakeRequest(routes.AmlRegulatedActivityLengthController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", amlActivityLength.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.AmountDueController.onPageLoad(NormalMode).url)
    }
  }

  s"POST ${routes.AmlRegulatedActivityLengthController.onSubmit(CheckMode).url}"   should {
    behave like authorisedActionRoute(routes.AmlRegulatedActivityLengthController.onSubmit(CheckMode))
    behave like goToNextPageInCheckMode(
      value = validLength,
      updateEclReturnValue = updateAmlActivityLength,
      clearEclReturnValue = clearAmlActivityLength,
      callToMake = routes.AmlRegulatedActivityLengthController.onSubmit(CheckMode),
      testSetup = Some(testSetup)
    )
  }
}
