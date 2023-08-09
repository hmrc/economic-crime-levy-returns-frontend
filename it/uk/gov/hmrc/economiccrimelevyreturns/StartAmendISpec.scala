package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, EclReturn, FirstTimeReturn, Obligation, ObligationData, ObligationDetails, Open}

import java.time.LocalDate

class StartAmendISpec extends ISpecBase with AuthorisedBehaviour{

  s"GET ${routes.StartAmendController.onPageLoad(":periodKey", ":chargeRef").url}" should {
    behave like authorisedActionRoute(routes.StartAmendController.onPageLoad(validPeriodKey, testChargeReference))

    "respond with 200 status and amend start HTML view if period key is valid" in {
      stubAuthorised()

      val openObligation = random[ObligationDetails].copy(
        status = Open,
        inboundCorrespondenceFromDate = LocalDate.parse("2022-04-01"),
        inboundCorrespondenceToDate = LocalDate.parse("2023-03-31"),
        periodKey = validPeriodKey
      )
      val obligationData = ObligationData(obligations = Seq(Obligation(Seq(openObligation))))
      val emptyReturn = EclReturn.empty(testInternalId, Some(AmendReturn))

      stubGetReturn(emptyReturn)

      stubGetObligations(obligationData)
      stubUpsertReturn(emptyReturn.copy(obligationDetails = Some(openObligation), returnType = Some(AmendReturn)))

      val result = callRoute(FakeRequest(routes.StartAmendController.onPageLoad(validPeriodKey, testChargeReference)))

      status(result) shouldBe OK
      html(result) should include ("Amend your Economic Crime Levy return for 2022 to 2023")
    }

    "respond with 200 status and no obligation for HTML view if there is no obligation data for provided period key" in {
      stubAuthorised()

      val obligationData = ObligationData(obligations = Seq.empty)

      stubGetObligations(obligationData)

      val result = callRoute(FakeRequest(routes.StartAmendController.onPageLoad(validPeriodKey, testChargeReference)))

      status(result) shouldBe OK
      html(result) should include ("You cannot submit a return for this financial year")
    }
  }
}