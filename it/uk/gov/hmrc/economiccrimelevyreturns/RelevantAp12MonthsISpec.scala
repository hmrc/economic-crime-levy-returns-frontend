package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, NormalMode, SessionData, SessionKeys}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._

class RelevantAp12MonthsISpec extends ISpecBase with AuthorisedBehaviour {

  def updateRelevantAp12Months(eclReturn: EclReturn, isFullYear: Boolean) =
    eclReturn.copy(
      relevantAp12Months = Some(isFullYear),
      relevantApLength = if (isFullYear) None else eclReturn.relevantApLength
    )

  def clearRelevantAp12Months(eclReturn: EclReturn) =
    eclReturn.copy(relevantAp12Months = None)

  def updateRelevantApLength(eclReturn: EclReturn, length: Int) =
    eclReturn.copy(relevantApLength = Some(length))

  def clearRelevantApLength(eclReturn: EclReturn) =
    eclReturn.copy(relevantApLength = None)

  def testSetup(internalId: String = testInternalId): Unit =
    stubGetSession(
      SessionData(
        internalId = internalId,
        values = Map(SessionKeys.PeriodKey -> testPeriodKey)
      )
    )

  s"GET ${routes.RelevantAp12MonthsController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.RelevantAp12MonthsController.onPageLoad(NormalMode))

    "respond with 200 status and the relevant AP 12 months view" in {
      stubAuthorised()

      stubGetReturn(clearRelevantAp12Months(random[EclReturn]))
      testSetup()
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.RelevantAp12MonthsController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Is your relevant accounting period 12 months?")
    }
  }

  s"POST ${routes.RelevantAp12MonthsController.onSubmit(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.RelevantAp12MonthsController.onSubmit(NormalMode))

    "save the selected option then redirect to the UK revenue page when the Yes option is selected" in {
      stubAuthorised()

      val eclReturn = clearRelevantAp12Months(random[EclReturn])

      testSetup()
      stubGetReturn(eclReturn)
      stubUpsertReturn(updateRelevantAp12Months(eclReturn, true))

      val result = callRoute(
        FakeRequest(routes.RelevantAp12MonthsController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "true"))
      )

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.UkRevenueController.onPageLoad(NormalMode).url)
    }

    "save the selected option then redirect to the relevant AP length page when the No option is selected" in {
      stubAuthorised()

      val eclReturn = clearRelevantAp12Months(random[EclReturn])

      testSetup()
      stubGetReturn(eclReturn)
      stubUpsertReturn(updateRelevantAp12Months(eclReturn, false))

      val result = callRoute(
        FakeRequest(routes.RelevantAp12MonthsController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "false"))
      )

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.RelevantApLengthController.onPageLoad(NormalMode).url)
    }
  }

  s"POST ${routes.RelevantAp12MonthsController.onSubmit(CheckMode).url}" should {
    behave like authorisedActionRoute(routes.RelevantAp12MonthsController.onSubmit(CheckMode))
    behave like goToNextPageInCheckMode(
      value = false,
      updateEclReturnValue = updateRelevantAp12Months,
      clearEclReturnValue = clearRelevantAp12Months,
      destination = routes.RelevantAp12MonthsController.onSubmit(CheckMode),
      testSetup = Some(testSetup),
      relatedValueInfo = Some(
        RelatedValueInfo(
          value = random[Int],
          updateEclReturnValue = updateRelevantApLength,
          clearEclReturnValue = clearRelevantApLength,
          destination = routes.RelevantApLengthController.onPageLoad(CheckMode)
        )
      )
    )
  }
}
