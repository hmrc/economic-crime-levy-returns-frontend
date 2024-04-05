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

class RelevantApLengthISpec extends ISpecBase with AuthorisedBehaviour {

  private def updateApLength(eclReturn: EclReturn, length: Int) =
    eclReturn.copy(relevantApLength = Some(length))

  private def clearApLength(eclReturn: EclReturn) =
    eclReturn.copy(relevantApLength = None)

  private def testSetup(eclReturn: EclReturn = blankReturn, internalId: String = testInternalId): EclReturn = {
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

  s"GET ${routes.RelevantApLengthController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.RelevantApLengthController.onPageLoad(NormalMode))

    "respond with 200 status and the relevant AP length view" in {
      stubAuthorised()

      stubGetReturn(testSetup(random[EclReturn]))
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.RelevantApLengthController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("How long is your relevant accounting period?")
    }
  }

  s"POST ${routes.RelevantApLengthController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.RelevantApLengthController.onSubmit(NormalMode))

    "save the relevant AP length then redirect to the UK revenue page" in {
      stubAuthorised()

      val eclReturn        = testSetup(random[EclReturn])
      val relevantApLength = validLength

      stubGetReturn(clearApLength(eclReturn))
      stubUpsertReturn(updateApLength(eclReturn, relevantApLength))

      val result = callRoute(
        FakeRequest(routes.RelevantApLengthController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", relevantApLength.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.UkRevenueController.onPageLoad(NormalMode).url)
    }
  }

  s"POST ${routes.RelevantApLengthController.onSubmit(CheckMode).url}"   should {
    behave like authorisedActionRoute(routes.RelevantApLengthController.onSubmit(CheckMode))
    behave like goToNextPageInCheckMode(
      value = validLength,
      updateEclReturnValue = updateApLength,
      clearEclReturnValue = clearApLength,
      callToMake = routes.RelevantApLengthController.onSubmit(CheckMode),
      testSetup = Some(testSetup)
    )
  }
}
