package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._

import java.time.LocalDate
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, NormalMode, ObligationDetails, SessionData, SessionKeys}

class AmendReasonISpec extends ISpecBase with AuthorisedBehaviour {

  def updateAmendReason(eclReturn: EclReturn, reason: String) =
    eclReturn.copy(amendReason = Some(reason))

  def clearAmendReason(eclReturn: EclReturn) =
    eclReturn.copy(amendReason = None)

  def testSetup(eclReturn: EclReturn = blankReturn, internalId: String = testInternalId): EclReturn = {
    stubGetSession(
      SessionData(
        internalId = internalId,
        values = Map(SessionKeys.PeriodKey -> testPeriodKey)
      )
    )
    eclReturn
  }

  s"GET ${routes.AmendReasonController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.AmendReasonController.onPageLoad(NormalMode))

    "respond with 200 status and the amendment reason HTML view" in {
      stubAuthorised()

      val fromFY            = random[LocalDate]
      val toFY              = random[LocalDate]
      val updatedObligation = random[ObligationDetails].copy(
        inboundCorrespondenceFromDate = fromFY,
        inboundCorrespondenceToDate = toFY
      )

      testSetup()
      stubGetReturn(
        clearAmendReason(random[EclReturn])
          .copy(obligationDetails = Some(updatedObligation))
      )

      val result = callRoute(FakeRequest(routes.AmendReasonController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Why are you requesting to amend your return?")
      html(result) should include(s"${fromFY.getYear.toString} to ${toFY.getYear.toString}")
    }
  }

  s"POST ${routes.AmendReasonController.onSubmit(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.AmendReasonController.onSubmit(NormalMode))

    "save the provided reason then redirect to the contact role page" in {
      stubAuthorised()

      val eclReturn = clearAmendReason(random[EclReturn])
      val reason    = stringsLongerThan(1).retryUntil(s => s == s.trim).sample.get

      testSetup()
      stubGetReturn(eclReturn)
      stubUpsertReturn(updateAmendReason(eclReturn, reason))

      val result = callRoute(
        FakeRequest(routes.AmendReasonController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", reason))
      )

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.RelevantAp12MonthsController.onPageLoad(NormalMode).url)
    }
  }

  s"POST ${routes.AmendReasonController.onSubmit(CheckMode).url}" should {
    behave like authorisedActionRoute(routes.AmendReasonController.onSubmit(CheckMode))
    behave like goToNextPageInCheckMode(
      value = random[String],
      updateEclReturnValue = updateAmendReason,
      clearEclReturnValue = clearAmendReason,
      callToMake = routes.AmendReasonController.onSubmit(CheckMode),
      testSetup = Some(testSetup)
    )
  }
}
