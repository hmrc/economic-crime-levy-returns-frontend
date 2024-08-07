package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode, ObligationData, SessionData, SessionKeys}

class AmountDueISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmountDueController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.AmountDueController.onPageLoad(NormalMode))

    "respond with 200 status and the ECL amount due view when the ECL return data is valid" in {
      stubAuthorised()

      val eclReturn        = random[ValidEclReturn].eclReturn
      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.periodKey -> testPeriodKey))
      val obligationData   = random[ObligationData]

      stubGetReturn(eclReturn)
      stubGetObligations(obligationData)
      stubGetSession(validSessionData)
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.AmountDueController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Amount of Economic Crime Levy you need to pay")
    }

    "redirect to the invalid data page when the ECL return data is invalid" in {
      stubAuthorised()

      val eclReturn        = random[EclReturn].copy(calculatedLiability = None, obligationDetails = None)
      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.periodKey -> testPeriodKey))
      val obligationData   = random[ObligationData]

      stubGetReturn(eclReturn)
      stubGetObligations(obligationData)
      stubGetSession(validSessionData)
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.AmountDueController.onPageLoad(NormalMode)))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
    }
  }

  s"POST ${routes.AmountDueController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.AmountDueController.onSubmit(NormalMode))

    "redirect to the who is completing this return page" in {
      stubAuthorised()

      val eclReturn        = random[ValidEclReturn].eclReturn
      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.periodKey -> testPeriodKey))
      val obligationData   = random[ObligationData]

      stubGetReturn(eclReturn)
      stubGetObligations(obligationData)
      stubGetSession(validSessionData)

      val result = callRoute(FakeRequest(routes.AmountDueController.onSubmit(NormalMode)))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.ContactNameController.onPageLoad(NormalMode).url)
    }
  }

}
