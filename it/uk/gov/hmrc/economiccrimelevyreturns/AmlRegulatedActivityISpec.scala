package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, NormalMode, SessionData, SessionKeys}

class AmlRegulatedActivityISpec extends ISpecBase with AuthorisedBehaviour {

  def updateAmlActivityForFullYear(eclReturn: EclReturn, isFullYear: Boolean) =
    eclReturn.copy(
      carriedOutAmlRegulatedActivityForFullFy = Some(isFullYear),
      amlRegulatedActivityLength = if (isFullYear) None else eclReturn.amlRegulatedActivityLength
    )

  def clearAmlActivityForFullYear(eclReturn: EclReturn) =
    eclReturn.copy(carriedOutAmlRegulatedActivityForFullFy = None)

  def updateAmlActivityLength(eclReturn: EclReturn, length: Int) =
    eclReturn.copy(amlRegulatedActivityLength = Some(length))

  def clearAmlActivityLength(eclReturn: EclReturn) =
    eclReturn.copy(amlRegulatedActivityLength = None)

  def testSetup(eclReturn: EclReturn = blankReturn, internalId: String = testInternalId): EclReturn = {
    stubGetSession(
      SessionData(
        internalId = internalId,
        values = Map(SessionKeys.PeriodKey -> testPeriodKey)
      )
    )
    updateContactName(eclReturn)
  }

  def validLength =
    Gen.chooseNum[Int](MinMaxValues.ApDaysMin, MinMaxValues.ApDaysMax).sample.get

  s"GET ${routes.AmlRegulatedActivityController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.AmlRegulatedActivityController.onPageLoad(NormalMode))

    "respond with 200 status and the Aml regulated HTML view" in {
      stubAuthorised()

      val eclReturn        = random[EclReturn]
      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.PeriodKey -> testPeriodKey))

      stubGetReturn(eclReturn)
      stubGetSession(validSessionData)
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.AmlRegulatedActivityController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Did you carry out AML-regulated activity for the full financial year?")
    }
  }

  s"POST ${routes.AmlRegulatedActivityController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.AmlRegulatedActivityController.onSubmit(NormalMode))

    "save the selected AML regulated activity option then redirect to the ECL amount due page when the Yes option is selected" in {
      stubAuthorised()

      val eclReturn        =
        random[EclReturn].copy(carriedOutAmlRegulatedActivityForFullFy = None, amlRegulatedActivityLength = None)
      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.PeriodKey -> testPeriodKey))

      stubGetReturn(eclReturn)
      stubGetSession(validSessionData)

      val updatedReturn = eclReturn.copy(
        carriedOutAmlRegulatedActivityForFullFy = Some(false)
      )

      stubUpsertReturn(updatedReturn)

      val result = callRoute(
        FakeRequest(routes.AmlRegulatedActivityController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.AmlRegulatedActivityLengthController.onPageLoad(NormalMode).url)
    }

    "save the selected AML regulated activity option then redirect to the AML regulated activity length page when the No option is selected" in {
      stubAuthorised()

      val eclReturn        =
        random[EclReturn].copy(
          internalId = testInternalId,
          carriedOutAmlRegulatedActivityForFullFy = None,
          amlRegulatedActivityLength = None
        )
      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.PeriodKey -> testPeriodKey))

      stubGetReturn(eclReturn)
      stubGetSession(validSessionData)

      val updatedReturn = eclReturn.copy(
        carriedOutAmlRegulatedActivityForFullFy = Some(false)
      )

      stubUpsertReturn(updatedReturn)

      val result = callRoute(
        FakeRequest(routes.AmlRegulatedActivityController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.AmlRegulatedActivityLengthController.onPageLoad(NormalMode).url)
    }
  }

  s"POST ${routes.AmlRegulatedActivityController.onSubmit(CheckMode).url}"   should {
    behave like authorisedActionRoute(routes.AmlRegulatedActivityController.onSubmit(CheckMode))
    behave like goToNextPageInCheckMode(
      value = false,
      updateEclReturnValue = updateAmlActivityForFullYear,
      clearEclReturnValue = clearAmlActivityForFullYear,
      callToMake = routes.AmlRegulatedActivityController.onSubmit(CheckMode),
      testSetup = Some(testSetup),
      relatedValueInfo = Some(
        RelatedValueInfo(
          value = validLength,
          updateEclReturnValue = updateAmlActivityLength,
          clearEclReturnValue = clearAmlActivityLength,
          destination = routes.AmlRegulatedActivityLengthController.onPageLoad(CheckMode)
        )
      )
    )
  }
}
