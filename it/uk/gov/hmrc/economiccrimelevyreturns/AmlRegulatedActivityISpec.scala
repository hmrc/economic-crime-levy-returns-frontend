package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode, SessionData, SessionKeys}

class AmlRegulatedActivityISpec extends ISpecBase with AuthorisedBehaviour {

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
}
