package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode, ObligationDetails}

import java.time.LocalDate

class AmendReasonISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmendReasonController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.AmendReasonController.onPageLoad(NormalMode))

    "respond with 200 status and the amendment reason HTML view" in {
      stubAuthorised()

      val eclReturn         = random[EclReturn]
      val obligationDetails = random[ObligationDetails]
      val fromFY            = random[LocalDate]
      val toFY              = random[LocalDate]
      val updatedObligation =
        obligationDetails.copy(inboundCorrespondenceFromDate = fromFY, inboundCorrespondenceToDate = toFY)
      val updatedReturn     = eclReturn.copy(obligationDetails = Some(updatedObligation))

      stubGetReturn(updatedReturn)
      stubGetSessionEmpty()

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

      val eclReturn         = random[EclReturn]
      val obligationDetails = random[ObligationDetails]
      val fromFY            = random[LocalDate]
      val toFY              = random[LocalDate]
      val reason            = nonEmptyString.sample.get.trim
      val updatedObligation =
        obligationDetails.copy(inboundCorrespondenceFromDate = fromFY, inboundCorrespondenceToDate = toFY)
      val updatedReturn     = eclReturn.copy(obligationDetails = Some(updatedObligation))

      stubGetReturn(updatedReturn)
      stubGetSessionEmpty()

      val returnWithAmendReason = updatedReturn.copy(amendReason = Some(reason))

      stubUpsertReturn(returnWithAmendReason)

      val result = callRoute(
        FakeRequest(routes.AmendReasonController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", reason))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.RelevantAp12MonthsController.onPageLoad(NormalMode).url)
    }
  }
}
